package dev.nucleusframework.composeswiftui.idea

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerEx
import com.intellij.icons.AllIcons
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.Separator
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.externalSystem.service.project.manage.ProjectDataImportListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.DumbAwareToggleAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootEvent
import com.intellij.openapi.roots.ModuleRootListener
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.JBColor
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.util.Alarm
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.WrapLayout
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPsiFactory
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Rectangle
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.HierarchyEvent
import java.awt.event.MouseWheelEvent
import java.beans.PropertyChangeListener
import java.io.File
import java.net.ServerSocket
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.Scrollable
import javax.swing.SwingConstants

/**
 * Right side of the split editor: the rendered previews of the file, like Android Studio's Compose preview. Renders
 * on demand, on save or while typing (see [PreviewSettings]), only while it is visible.
 */
class PreviewEditor(private val project: Project, private val file: VirtualFile) : UserDataHolderBase(), FileEditor {
    // Resolved on use: the module is unknown until the Gradle sync has finished.
    val module: PreviewModule? get() = PreviewModule.of(project, file)

    private val renderer get() = project.service<PreviewRenderer>()
    private val options get() = PreviewSettings.options

    private var entries: List<PreviewEntry> = emptyList()

    /** Qualified names of the previews of the file, from the PSI and the last Gradle run. */
    private var previewIds: List<String> = emptyList()
    private var selectedKey: String? = null
    private var groupFilter: String? = null
    private var layout = options.layout

    /** Points to logical pixels; null fits the previews to the pane. */
    private var zoom: Double? = null
    private var rendering = false
    private var problems: List<BuildProblem> = emptyList()
    private var lastOutcome: RenderOutcome? = null
    /** Fallback when the built text is unknown: the images are older than the file. */
    private var imagesOlderThanFile = false

    /** A render was asked for while the pane was hidden: done when it shows. */
    private var renderWhenShown = false

    /** What the current render is doing, from Gradle's tasks. */
    private var progressStep = "Rendering…"

    /** The file as last built: live literal edits are computed against it. */
    private var compiledText: String? = null
    private var buildingText: String? = null

    /** Literal edits since the last build, shown by the running host without a build (live literals). */
    private var liveEdits: List<LiteralEdit> = emptyList()
    private var renderedLiveEdits: List<LiteralEdit> = emptyList()
    private var liveRendering = false
    private val liveAlarm = Alarm(Alarm.ThreadToUse.SWING_THREAD, this)

    private var interactive: InteractivePreview? = null
    private var navigatorCache: Pair<PreviewModule, SourceNavigator>? = null
    private val navigator: SourceNavigator?
        get() = module?.let { m -> navigatorCache?.takeIf { it.first == m }?.second ?: SourceNavigator(project, m).also { navigatorCache = m to it } }

    private val status = JBLabel().apply {
        foreground = UIUtil.getContextHelpForeground()
        horizontalTextPosition = SwingConstants.RIGHT
        border = JBUI.Borders.emptyRight(8)
    }
    private val problemsPanel = JPanel(VerticalLayout(JBUI.scale(4))).apply {
        background = JBUI.CurrentTheme.Banner.ERROR_BACKGROUND
        border = JBUI.Borders.compound(
            JBUI.Borders.customLineBottom(JBUI.CurrentTheme.Banner.ERROR_BORDER_COLOR),
            JBUI.Borders.empty(8, 12),
        )
        isVisible = false
    }
    private val cards = CardsPanel()
    private val scrollPane = ZoomableScrollPane(cards) { factor -> zoomBy(factor) }
    private val component = JPanel(BorderLayout())
    private val autoRenderAlarm = Alarm(Alarm.ThreadToUse.SWING_THREAD, this)
    private val typingAlarm = Alarm(Alarm.ThreadToUse.SWING_THREAD, this)

    private val moduleAlarm = Alarm(Alarm.ThreadToUse.SWING_THREAD, this)

    /** Previews rendered because a reload found them missing: once each, even if they still have no result. */
    private val renderedMissing = HashSet<String>()

    /** The last save didn't build because the file had errors. */
    private var blockedByErrors = false

    /** Whether the previews were checked for a render on open, once the module is known. */
    private var openRenderChecked = false

    /** Identity of this editor's reloads: a newer one replaces a pending one. */
    private val reloadKey = Any()

    init {
        val toolbar = ActionManager.getInstance()
            .createActionToolbar("ComposeSwiftUIPreview", toolbarActions(), true)
            .apply { targetComponent = component }
        val header = JPanel(BorderLayout()).apply {
            add(toolbar.component, BorderLayout.CENTER)
            add(status, BorderLayout.EAST)
            border = JBUI.Borders.customLineBottom(JBUI.CurrentTheme.ToolWindow.borderColor())
        }
        val top = JPanel(BorderLayout()).apply {
            add(header, BorderLayout.NORTH)
            add(problemsPanel, BorderLayout.CENTER)
        }
        component.add(top, BorderLayout.NORTH)
        component.add(scrollPane, BorderLayout.CENTER)

        scrollPane.viewport.addComponentListener(object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent) {
                if (zoom == null) applyZoom()
            }
        })
        component.addHierarchyListener { e ->
            if (e.changeFlags and HierarchyEvent.SHOWING_CHANGED.toLong() != 0L && component.isShowing && renderWhenShown) {
                renderWhenShown = false
                render()
            }
        }

        val connection = project.messageBus.connect(this)
        connection.subscribe(PreviewRenderListener.TOPIC, object : PreviewRenderListener {
            override fun renderingStarted(module: PreviewModule, ids: Set<String>) {
                if (module != this@PreviewEditor.module) return
                rendering = true
                // What Gradle compiles: the file on disk.
                buildingText = diskText()
                updateStatus()
            }

            override fun renderingProgress(module: PreviewModule, step: String) {
                if (module != this@PreviewEditor.module || !rendering) return
                progressStep = step
                updateStatus()
            }

            override fun renderingFinished(module: PreviewModule, ids: Set<String>, outcome: RenderOutcome) {
                LOG.debug("Render finished for ${file.name}: same module=${module == this@PreviewEditor.module}")
                if (module != this@PreviewEditor.module) return
                rendering = renderer.isRendering(module)
                progressStep = "Starting Gradle…"
                lastOutcome = outcome
                problems = outcome.problems
                // Only a build changes what the host runs: a render of the last build keeps its text.
                if (outcome.success && outcome.built) {
                    compiledText = buildingText
                    buildingText?.let { text -> BuiltSources.save(module, file.path, text) }
                    liveEdits = emptyList()
                    renderedLiveEdits = emptyList()
                }
                updateProblems()
                reload()
                // Literals edited during the render: against the new build, or skipped while it ran.
                updateLiveLiterals()
                renderLiveEdits()
            }
        })
        connection.subscribe(ProjectDataImportListener.TOPIC, object : ProjectDataImportListener {
            override fun onImportFinished(projectPath: String?) {
                ApplicationManager.getApplication().invokeLater { if (!project.isDisposed) reload() }
            }
        })
        // Auto render: a saved Kotlin file of the same Gradle build can change what the previews show.
        connection.subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener {
            override fun after(events: List<VFileEvent>) {
                if (options.autoRender == AutoRender.MANUAL) return
                val root = module?.rootProjectPath ?: return
                // Generated sources (rewritten by the render itself) are not edits.
                val changed = events.filter { it is VFileContentChangeEvent && it.file.extension == "kt" && it.path.startsWith(root) && "/build/" !in it.path }
                if (changed.isEmpty()) return
                // Literal edits of this file are already shown by the running host (live literals): no build for them.
                if (changed.all { it.file == file } && literalsOnlySinceBuild()) {
                    LOG.info("${file.name} saved with literal edits only: shown live, no build")
                    updateLiveLiterals()
                    return
                }
                if (changed.any { it.file == file } && hasErrors()) {
                    LOG.info("${file.name} saved with errors: no build until they are fixed")
                    blockedByErrors = true
                    updateStatus()
                    return
                }
                blockedByErrors = false
                LOG.info("Building for ${changed.map { it.path.substringAfterLast("/") }} (compiled text known: ${compiledText != null})")
                autoRenderAlarm.cancelAllRequests()
                autoRenderAlarm.addRequest({ requestRender() }, 200)
            }
        })
        // Render while typing: once the code is analyzed and has no error, save it, which builds (file listener).
        connection.subscribe(DaemonCodeAnalyzer.DAEMON_EVENT_TOPIC, object : DaemonCodeAnalyzer.DaemonListener {
            override fun daemonFinished(fileEditors: Collection<FileEditor>) {
                if (options.autoRender == AutoRender.MANUAL || fileEditors.none { it.file == file }) return
                val document = FileDocumentManager.getInstance().getDocument(file) ?: return
                if (sync() != Sync.OUT_OF_DATE || hasErrors()) return
                if (!FileDocumentManager.getInstance().isDocumentUnsaved(document)) {
                    // Saved while the analysis still showed errors, now gone: the build the save skipped.
                    if (blockedByErrors) {
                        blockedByErrors = false
                        requestRender()
                    }
                    return
                }
                if (options.autoRender != AutoRender.WHILE_TYPING) return
                typingAlarm.cancelAllRequests()
                typingAlarm.addRequest({ FileDocumentManager.getInstance().saveDocument(document) }, options.typingDelayMs)
            }
        })
        FileDocumentManager.getInstance().getDocument(file)?.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                liveAlarm.cancelAllRequests()
                liveAlarm.addRequest({
                    updateLiveLiterals()
                    updateStatus()
                }, 120)
                // While typing, a structural edit builds once the analysis of the file ends without errors (see
                // the daemon listener): half-typed code ("fa" on the way to "false") never builds.
                typingAlarm.cancelAllRequests()
            }
        }, this)

        // Modules appear after the project is loaded (from the sync cache, without an import).
        connection.subscribe(ModuleRootListener.TOPIC, object : ModuleRootListener {
            override fun rootsChanged(event: ModuleRootEvent) {
                if (module == null || !openRenderChecked) reload()
            }
        })
        reload()
    }

    /** Selects the preview of the function under the caret of the code editor, like Android Studio. */
    fun attach(textEditor: TextEditor) {
        textEditor.editor.caretModel.addCaretListener(object : CaretListener {
            override fun caretPositionChanged(event: CaretEvent) {
                val document = event.editor.document
                if (PsiDocumentManager.getInstance(project).isUncommited(document)) return
                val offset = event.editor.caretModel.offset
                val id = ReadAction.compute<String?, RuntimeException> {
                    val psi = PsiDocumentManager.getInstance(project).getPsiFile(document) as? KtFile
                    val function = psi?.let { PsiTreeUtil.getParentOfType(it.findElementAt(offset), KtNamedFunction::class.java) }
                    function?.takeIf { it.isTopLevel }?.fqName?.asString()
                } ?: return
                val entry = entries.firstOrNull { it.id == id } ?: return
                if (entries.none { it.id == id && it.key == selectedKey }) select(entry.key, scroll = true)
            }
        }, this)
    }

    /** Renders the previews of this file. */
    fun render() {
        val module = module ?: return
        // The previews of the code now: added or renamed since the last reload.
        val ids = previewIds + currentPreviewIds()
        if (ids.isEmpty()) return
        renderer.render(module, ids.toSet())
    }

    private fun currentPreviewIds(): List<String> {
        val text = FileDocumentManager.getInstance().getDocument(file)?.text ?: return emptyList()
        // From the text: the PSI may not be committed yet, and only the syntax matters.
        return ReadAction.compute<List<String>, RuntimeException> {
            KtPsiFactory(project).createFile(file.name, text).previewFunctions().mapNotNull { it.fqName?.asString() }
        }
    }

    /** Renders one preview function. */
    fun render(id: String) {
        module?.let { renderer.render(it, setOf(id)) }
    }

    /** Selects and scrolls to the first preview of [qualifiedName]. */
    fun show(qualifiedName: String) {
        entries.firstOrNull { it.id == qualifiedName }?.let { select(it.key, scroll = true) }
    }

    /** Renders now when the pane shows, otherwise when it is shown. */
    private fun requestRender() {
        if (component.isShowing || ApplicationManager.getApplication().isUnitTestMode) render() else renderWhenShown = true
    }

    /**
     * Reads the file's previews and their images off the EDT (PSI and disk access), then shows them. Retries while
     * the module is unknown, and renders what is missing or stale the first time it is known, like Android Studio
     * opening a file with up-to-date previews.
     */
    private fun reload() {
        ReadAction.nonBlocking<Triple<PreviewModule?, List<String>, List<PreviewEntry>>> {
            val module = module
            val ktFile = PsiManager.getInstance(project).findFile(file) as? KtFile ?: return@nonBlocking Triple(module, emptyList(), emptyList())
            val functions = ktFile.previewFunctions()
            val titles = functions.associate { it.fqName?.asString().orEmpty() to it.previewTitle() }
            val ids = (functions.mapNotNull { it.fqName?.asString() } + module?.let { PreviewFiles.manifestIds(it, file.path) }.orEmpty())
                .distinct()
            val loaded = PreviewEntry.load(module, ids, titles, file)
            navigator?.prefetch(loaded.mapNotNull { it.layout })
            Triple(module, ids, loaded)
        }
            .expireWith(this)
            .coalesceBy(reloadKey)
            .finishOnUiThread(ModalityState.any()) { (module, ids, loaded) ->
                LOG.debug("Previews of ${file.name}: module=${module?.gradlePath}, ${ids.size} preview(s), ${loaded.count { it.rendered }}/${loaded.size} rendered")
                previewIds = ids
                entries = loaded
                imagesOlderThanFile = PreviewEntry.isOutOfDate(loaded, file)
                // Up-to-date images: the file on disk is what the running host was built from.
                // After a restart: the text of the build the host runs, if still that build.
                if (compiledText == null && module != null) {
                    compiledText = BuiltSources.load(module, file.path)
                    // Literal edits made meanwhile: shown live.
                    if (compiledText != null) liveAlarm.addRequest({ updateLiveLiterals(); updateStatus() }, 0)
                }
                if (!imagesOlderThanFile && compiledText == null && loaded.any { it.rendered }) compiledText = diskText()
                if (selectedKey !in loaded.map { it.key }) selectedKey = loaded.firstOrNull()?.key
                rebuildCards()
                updateStatus()
                // Previews only the last build found (a new custom multipreview): rendered once, now that it knows them.
                val missing = loaded.filter { !it.rendered }.map { it.id }.toSet() - renderedMissing
                val renderMissing = openRenderChecked && missing.isNotEmpty() && lastOutcome?.success == true &&
                    module != null && !renderer.isRendering(module) && options.autoRender != AutoRender.MANUAL
                if (renderMissing) {
                    renderedMissing += missing
                    renderer.render(module!!, missing)
                }
                when {
                    module == null -> moduleAlarm.addRequest({ reload() }, 2000)
                    !openRenderChecked -> {
                        openRenderChecked = true
                        if (options.autoRender != AutoRender.MANUAL && loaded.isNotEmpty() && (sync() == Sync.OUT_OF_DATE || loaded.any { !it.rendered })) {
                            requestRender()
                        }
                    }
                }
            }
            .submit(AppExecutorUtil.getAppExecutorService())
    }

    private fun visibleEntries(): List<PreviewEntry> {
        val filtered = entries.filter { groupFilter == null || it.group == groupFilter }
        return when (layout) {
            PreviewLayout.GALLERY -> listOfNotNull(filtered.firstOrNull { it.key == selectedKey } ?: filtered.firstOrNull())
            else -> filtered
        }
    }

    private fun rebuildCards() {
        cards.removeAll()
        cards.layout = when (layout) {
            PreviewLayout.GRID -> WrapLayout(FlowLayout.LEFT, JBUI.scale(20), JBUI.scale(20))
            // Cards keep their width: the selection outline hugs the preview.
            else -> VerticalLayout(JBUI.scale(24), SwingConstants.LEFT)
        }
        cards.border = JBUI.Borders.empty(12)
        val visible = visibleEntries()
        for (entry in visible) {
            cards.add(PreviewCard(entry, selected = entry.key == selectedKey, navigator = navigator, actions = { cardActions(entry) }, onSelect = { navigate ->
                select(entry.key, scroll = false)
                navigateTo(entry.id, focus = navigate)
            }))
        }
        if (visible.isEmpty()) {
            val message = when {
                module == null -> "Waiting for the Gradle sync…"
                entries.isEmpty() -> "No @Preview composable in this file"
                else -> "No preview in this group"
            }
            cards.add(JBLabel(message).apply { foreground = UIUtil.getContextHelpForeground() })
        }
        applyZoom()
    }

    private fun applyZoom() {
        val viewport = scrollPane.viewport.extentSize
        val available = Dimension(viewport.width - JBUI.scale(48), viewport.height - JBUI.scale(96))
        for (card in cards.components.filterIsInstance<PreviewCard>()) {
            val entry = card.entry
            val scale = zoom ?: fitScale(entry, available)
            card.setScale(scale)
        }
        cards.revalidate()
        cards.repaint()
    }

    private fun fitScale(entry: PreviewEntry, available: Dimension): Double {
        if (entry.width <= 0) return 1.0
        val byWidth = available.width.toDouble() / entry.width
        return when (layout) {
            PreviewLayout.LIST -> byWidth.coerceIn(0.1, 1.0)
            PreviewLayout.GRID -> (JBUI.scale(240).toDouble() / entry.width).coerceAtMost(1.0)
            PreviewLayout.GALLERY -> minOf(byWidth, available.height.toDouble() / entry.height.coerceAtLeast(1)).coerceIn(0.1, 2.0)
        }
    }

    private fun currentScale(): Double {
        val card = cards.components.filterIsInstance<PreviewCard>().firstOrNull() ?: return 1.0
        return card.scale
    }

    private fun zoomBy(factor: Double) {
        zoom = (currentScale() * factor).coerceIn(0.1, 4.0)
        applyZoom()
    }

    private fun select(key: String, scroll: Boolean) {
        selectedKey = key
        if (layout == PreviewLayout.GALLERY) {
            rebuildCards()
            return
        }
        for (card in cards.components.filterIsInstance<PreviewCard>()) card.setSelected(card.entry.key == key)
        if (scroll) {
            cards.components.filterIsInstance<PreviewCard>().firstOrNull { it.entry.key == key }?.let { card ->
                cards.scrollRectToVisible(Rectangle(card.x, card.y, card.width, card.height.coerceAtMost(scrollPane.viewport.height)))
            }
        }
    }

    private fun navigateTo(id: String, focus: Boolean) {
        // The UI thread has no implicit read access to the PSI.
        val offset = ReadAction.compute<Int?, RuntimeException> {
            val ktFile = PsiManager.getInstance(project).findFile(file) as? KtFile
            ktFile?.findFunction(id)?.let { it.nameIdentifier?.textOffset ?: it.textOffset }
        } ?: return
        com.intellij.openapi.fileEditor.OpenFileDescriptor(project, file, offset).navigate(focus)
    }

    private enum class Sync { UP_TO_DATE, APPLYING_LITERALS, OUT_OF_DATE }

    // For tests: what the pane shows.
    internal val statusText: String get() = status.text.orEmpty()
    internal val shownEntries: List<PreviewEntry> get() = entries
    internal val shownProblems: List<BuildProblem> get() = problems.takeIf { problemsPanel.isVisible }.orEmpty()

    /** The editor shows an error in the file (syntax or resolution): a build would fail. */
    private fun hasErrors(): Boolean {
        val document = FileDocumentManager.getInstance().getDocument(file) ?: return false
        var found = false
        DaemonCodeAnalyzerEx.processHighlights(document, project, HighlightSeverity.ERROR, 0, document.textLength) {
            found = true
            false
        }
        return found
    }

    /**
     * How the previews compare to the code in the editor: the text of the last build plus the literal edits the host
     * shows. Without a known build (the IDE just opened), the image and file dates.
     */
    private fun sync(): Sync {
        val compiled = compiledText ?: return if (imagesOlderThanFile) Sync.OUT_OF_DATE else Sync.UP_TO_DATE
        val current = FileDocumentManager.getInstance().getDocument(file)?.text ?: return Sync.UP_TO_DATE
        if (current == compiled) return Sync.UP_TO_DATE
        val edits = LiteralDiff.edits(file.name, compiled, current) { null } ?: return Sync.OUT_OF_DATE
        if (options.autoRender == AutoRender.MANUAL) return Sync.OUT_OF_DATE
        return if (edits.map { it.offset to it.new } == renderedLiveEdits.map { it.offset to it.new }) Sync.UP_TO_DATE else Sync.APPLYING_LITERALS
    }

    private fun updateStatus() {
        val outcome = lastOutcome
        val sync = sync()
        when {
            rendering -> {
                status.icon = AnimatedIcon.Default.INSTANCE
                status.text = progressStep
                status.foreground = UIUtil.getContextHelpForeground()
            }
            outcome != null && !outcome.success -> {
                status.icon = AllIcons.General.Error
                status.text = "Render failed"
                status.foreground = JBColor.RED
            }
            liveRendering || sync == Sync.APPLYING_LITERALS -> {
                status.icon = AnimatedIcon.Default.INSTANCE
                status.text = "Applying literal edits…"
                status.foreground = UIUtil.getContextHelpForeground()
            }
            sync == Sync.UP_TO_DATE && liveEdits.isNotEmpty() -> {
                status.icon = AllIcons.Actions.Lightning
                status.text = "Up to date (${liveEdits.size} literal edit(s) applied live)"
                status.foreground = UIUtil.getContextHelpForeground()
            }
            sync == Sync.OUT_OF_DATE && hasErrors() -> {
                status.icon = AllIcons.General.Warning
                status.text = "Out of date: fix the errors to render"
                status.foreground = UIUtil.getContextHelpForeground()
            }
            sync == Sync.OUT_OF_DATE -> {
                status.icon = AllIcons.General.Warning
                status.text = when (options.autoRender) {
                    AutoRender.MANUAL -> "Out of date: render to update"
                    AutoRender.ON_SAVE -> "Out of date: built on save"
                    AutoRender.WHILE_TYPING -> "Out of date: building when the code is valid"
                }
                status.foreground = UIUtil.getContextHelpForeground()
            }
            outcome != null -> {
                status.icon = null
                status.text = "Rendered in %.1f s".format(outcome.milliseconds / 1000.0)
                status.foreground = UIUtil.getContextHelpForeground()
            }
            else -> {
                status.icon = null
                status.text = if (module == null) "Waiting for the Gradle sync…" else ""
            }
        }
    }

    private fun updateProblems() {
        problemsPanel.removeAll()
        if (problems.isNotEmpty()) {
            problemsPanel.add(JBLabel("The previews couldn't be rendered", AllIcons.General.Error, SwingConstants.LEFT).apply {
                font = JBUI.Fonts.label().asBold()
            })
            for (problem in problems.take(MAX_PROBLEMS)) {
                val path = problem.path
                problemsPanel.add(
                    if (path != null) {
                        ActionLink("${File(path).name}:${problem.line}: ${problem.message}") { openProblem(problem) }
                    } else {
                        JBLabel("<html>${problem.message.escapeHtml().replace("\n", "<br>")}</html>")
                    },
                )
            }
            if (problems.size > MAX_PROBLEMS) problemsPanel.add(JBLabel("${problems.size - MAX_PROBLEMS} more in the Run tool window"))
        }
        problemsPanel.isVisible = problems.isNotEmpty()
        problemsPanel.revalidate()
        updateStatus()
    }

    private fun openProblem(problem: BuildProblem) {
        val target = VirtualFileManager.getInstance().findFileByNioPath(File(problem.path ?: return).toPath()) ?: return
        com.intellij.openapi.fileEditor.OpenFileDescriptor(project, target, (problem.line - 1).coerceAtLeast(0), (problem.column - 1).coerceAtLeast(0))
            .navigate(true)
    }

    private fun diskText(): String? = runCatching { String(file.contentsToByteArray(), file.charset) }.getOrNull()

    /** The saved file differs from the last build by literals only (shown by live literals, so no build is needed). */
    private fun literalsOnlySinceBuild(): Boolean {
        val compiled = compiledText ?: return false
        val saved = diskText() ?: return false
        return LiteralDiff.edits(file.name, compiled, saved) { null } != null
    }


    /**
     * Live literals: when only literals changed since the last build, the running host shows them right away, in the
     * live preview and in the images, then the build (on save) replaces them.
     */
    private fun updateLiveLiterals() {
        if (options.autoRender == AutoRender.MANUAL) return
        val compiled = compiledText ?: return
        val current = FileDocumentManager.getInstance().getDocument(file)?.text ?: return
        val edits = LiteralDiff.edits(file.name, compiled, current) { offset -> enclosingFunction(current, offset) } ?: return
        // Back to the last build give or take literals: the code that failed to build is gone.
        if (lastOutcome?.success == false && !rendering && failedInThisFile()) {
            lastOutcome = null
            problems = emptyList()
            updateProblems()
        }
        if (edits == liveEdits) return
        liveEdits = edits
        interactive?.patch(edits)
        updateStatus()
        renderLiveEdits()
    }

    /** Renders the images with the latest literal edits on the running host, one request at a time. */
    private fun renderLiveEdits() {
        val module = module ?: return
        if (liveRendering || liveEdits == renderedLiveEdits || renderer.isRendering(module) || previewIds.isEmpty()) return
        liveRendering = true
        updateStatus()
        val edits = liveEdits
        val ids = previewIds.toSet()
        ApplicationManager.getApplication().executeOnPooledThread {
            val done = PreviewHostClient(module).render(ids, edits, options.settleMs)
            ApplicationManager.getApplication().invokeLater {
                if (project.isDisposed) return@invokeLater
                liveRendering = false
                if (done) {
                    renderedLiveEdits = edits
                    reload()
                } else {
                    // No host of the last build is running (it was stopped, or the IDE restarted): build instead.
                    LOG.info("No running host for live literals: building")
                    compiledText = null
                    render()
                }
                updateStatus()
                // Edited again meanwhile.
                if (done && liveEdits != edits) renderLiveEdits()
            }
        }
    }

    private fun failedInThisFile(): Boolean {
        val located = problems.mapNotNull { it.path }
        val path = File(file.path).canonicalPath
        return located.isNotEmpty() && located.all { File(it).canonicalPath == path }
    }

    private fun enclosingFunction(text: String, offset: Int): String? =
        Regex("""\bfun\s+(?:<[^>]*>\s*)?(?:[\w.]+\.)?(\w+)\s*\(""").findAll(text.substring(0, offset.coerceAtMost(text.length)))
            .lastOrNull()?.groupValues?.get(1)

    /**
     * Interactive mode: the preview [key] runs live on the simulator, in place of the images. Builds first only when
     * the code changed beyond literals since the last build.
     */
    fun startInteractive(key: String) {
        val module = module ?: return
        val navigator = navigator ?: return
        interactive?.let { Disposer.dispose(it) }
        val title = entries.firstOrNull { it.key == key || it.id == key.substringBefore('#') }?.title ?: key
        val panel = InteractivePreview(title, navigator) { closeInteractive() }
        Disposer.register(this, panel)
        interactive = panel
        component.remove(scrollPane)
        component.add(panel, BorderLayout.CENTER)
        component.revalidate()
        component.repaint()

        val port = ServerSocket(0).use { it.localPort }
        val edits = liveEdits
        val start = { PreviewHostClient(module).startInteractive(key, port, edits) }
        val connectOrBuild = {
            if (start()) {
                ApplicationManager.getApplication().invokeLater { panel.connect(port) }
            } else {
                ApplicationManager.getApplication().invokeLater {
                    renderer.render(module, setOf(key.substringBefore('#').substringBefore('~'))) { outcome ->
                        if (!outcome.success) return@render panel.failed("The build failed: see the problems above")
                        ApplicationManager.getApplication().executeOnPooledThread {
                            if (start()) {
                                ApplicationManager.getApplication().invokeLater { panel.connect(port) }
                            } else {
                                ApplicationManager.getApplication().invokeLater { panel.failed("The preview host didn't start") }
                            }
                        }
                    }
                }
            }
        }
        val needsBuild = sync() == Sync.OUT_OF_DATE
        if (needsBuild) {
            renderer.render(module, setOf(key.substringBefore('#').substringBefore('~'))) { outcome ->
                if (outcome.success) ApplicationManager.getApplication().executeOnPooledThread { connectOrBuild() }
                else panel.failed("The build failed: see the problems above")
            }
        } else {
            ApplicationManager.getApplication().executeOnPooledThread { connectOrBuild() }
        }
    }

    private fun closeInteractive() {
        val panel = interactive ?: return
        interactive = null
        component.remove(panel)
        component.add(scrollPane, BorderLayout.CENTER)
        component.revalidate()
        component.repaint()
    }

    private fun cardActions(entry: PreviewEntry): ActionGroup = DefaultActionGroup(
        action("Start Interactive Mode", AllIcons.Actions.Execute, enabled = module != null && entry.rendered && entry.error == null) {
            startInteractive(entry.key)
        },
        action("Go to Source", AllIcons.Actions.EditSource) { navigateTo(entry.id, focus = true) },
        action("Render This Preview", AllIcons.Actions.Refresh, enabled = module != null) { render(entry.id) },
        action("Run Preview", AllIcons.Nodes.Deploy, enabled = module != null && entry.rendered) {
            module?.let { renderer.runInteractive(it, entry.key) }
        },
        Separator.getInstance(),
        action("Copy Image", AllIcons.Actions.Copy, enabled = entry.image != null) { PreviewImages.copy(entry) },
        action("Save Image As…", AllIcons.Actions.MenuSaveall, enabled = entry.image != null) { PreviewImages.save(project, entry) },
        action("Copy Error", AllIcons.Actions.Copy, enabled = entry.error != null) { PreviewImages.copyText(entry.error.orEmpty()) },
    )

    private fun toolbarActions(): ActionGroup {
        val renderAction = object : DumbAwareAction("Render Previews", "Render the previews of this file on the iOS simulator or the Mac", AllIcons.Actions.Refresh) {
            override fun actionPerformed(e: AnActionEvent) = render()
            override fun update(e: AnActionEvent) {
                val module = module
                e.presentation.isEnabled = module != null && previewIds.isNotEmpty() && !renderer.isRendering(module)
            }
            override fun getActionUpdateThread() = ActionUpdateThread.EDT
        }.apply { copyShortcutFrom(ActionManager.getInstance().getAction(RenderPreviewsAction.ID)) }

        val autoRender = DefaultActionGroup("Render Automatically", AutoRender.entries.map { mode ->
            object : DumbAwareToggleAction(mode.label) {
                override fun isSelected(e: AnActionEvent) = options.autoRender == mode
                override fun setSelected(e: AnActionEvent, state: Boolean) {
                    if (state) options.autoRender = mode
                    updateStatus()
                }
                override fun getActionUpdateThread() = ActionUpdateThread.EDT
            }
        }).apply {
            isPopup = true
            templatePresentation.icon = AllIcons.Actions.Lightning
        }

        val layouts = PreviewLayout.entries.map { mode ->
            object : DumbAwareToggleAction(mode.name.lowercase().replaceFirstChar { it.uppercase() }, null, mode.icon()) {
                override fun isSelected(e: AnActionEvent) = layout == mode
                override fun setSelected(e: AnActionEvent, state: Boolean) {
                    if (!state) return
                    layout = mode
                    options.layout = mode
                    zoom = null
                    rebuildCards()
                }
                override fun getActionUpdateThread() = ActionUpdateThread.EDT
            }
        }

        val groups = object : DefaultActionGroup("Group", true) {
            override fun getChildren(e: AnActionEvent?): Array<AnAction> {
                val names = listOf<String?>(null) + entries.map { it.group }.filter { it.isNotEmpty() }.distinct().sorted()
                return names.map { name ->
                    object : DumbAwareToggleAction(name ?: "All Groups") {
                        override fun isSelected(e: AnActionEvent) = groupFilter == name
                        override fun setSelected(e: AnActionEvent, state: Boolean) {
                            groupFilter = name
                            rebuildCards()
                        }
                        override fun getActionUpdateThread() = ActionUpdateThread.EDT
                    }
                }.toTypedArray()
            }

            override fun update(e: AnActionEvent) {
                e.presentation.isVisible = entries.any { it.group.isNotEmpty() }
                e.presentation.text = "Group: ${groupFilter ?: "All"}"
            }

            override fun getActionUpdateThread() = ActionUpdateThread.EDT
        }.apply { templatePresentation.icon = AllIcons.General.Filter }

        val previous = galleryStep("Previous Preview", AllIcons.Actions.Back, -1)
        val next = galleryStep("Next Preview", AllIcons.Actions.Forward, 1)

        return DefaultActionGroup(
            renderAction,
            autoRender,
            Separator.getInstance(),
            *layouts.toTypedArray(),
            groups,
            previous,
            next,
            Separator.getInstance(),
            action("Zoom In", AllIcons.General.ZoomIn) { zoomBy(1.25) },
            action("Zoom Out", AllIcons.General.ZoomOut) { zoomBy(0.8) },
            action("Actual Size (1 point per pixel)", AllIcons.General.ActualZoom) { zoom = 1.0; applyZoom() },
            action("Zoom to Fit", AllIcons.General.FitContent) { zoom = null; applyZoom() },
            Separator.getInstance(),
            action("Settings…", AllIcons.General.Settings) { ShowSettingsUtil.getInstance().showSettingsDialog(project, PreviewConfigurable::class.java) },
        )
    }

    private fun galleryStep(text: String, icon: javax.swing.Icon, step: Int) = object : DumbAwareAction(text, null, icon) {
        override fun actionPerformed(e: AnActionEvent) {
            val list = entries.filter { groupFilter == null || it.group == groupFilter }
            if (list.isEmpty()) return
            val index = list.indexOfFirst { it.key == selectedKey }.coerceAtLeast(0)
            select(list[(index + step).mod(list.size)].key, scroll = false)
        }

        override fun update(e: AnActionEvent) {
            e.presentation.isVisible = layout == PreviewLayout.GALLERY
            e.presentation.isEnabled = entries.size > 1
        }

        override fun getActionUpdateThread() = ActionUpdateThread.EDT
    }

    private fun PreviewLayout.icon() = when (this) {
        PreviewLayout.LIST -> AllIcons.Actions.SplitHorizontally
        PreviewLayout.GRID -> AllIcons.Actions.GroupByModule
        PreviewLayout.GALLERY -> AllIcons.Actions.Preview
    }

    override fun getComponent(): JComponent = component
    override fun getPreferredFocusedComponent(): JComponent = cards
    override fun getName() = "Preview"
    override fun getFile() = file
    override fun setState(state: FileEditorState) {}
    override fun isModified() = false
    override fun isValid() = file.isValid
    override fun addPropertyChangeListener(listener: PropertyChangeListener) {}
    override fun removePropertyChangeListener(listener: PropertyChangeListener) {}
    override fun dispose() {}

    private companion object {
        val LOG = logger<PreviewEditor>()
        const val MAX_PROBLEMS = 8
    }
}

/** A toolbar or menu action running [run] on the EDT. */
internal fun action(text: String, icon: javax.swing.Icon?, enabled: Boolean = true, run: () -> Unit): AnAction =
    object : DumbAwareAction(text, null, icon) {
        override fun actionPerformed(e: AnActionEvent) = run()
        override fun update(e: AnActionEvent) {
            e.presentation.isEnabled = enabled
        }
        override fun getActionUpdateThread() = ActionUpdateThread.EDT
    }

internal fun String.escapeHtml() = replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

/** Cards panel that wraps (grid) to the width of the viewport instead of growing sideways. */
private class CardsPanel : JPanel(), Scrollable {
    init {
        isOpaque = false
    }

    override fun getPreferredScrollableViewportSize(): Dimension = preferredSize
    override fun getScrollableUnitIncrement(visibleRect: Rectangle, orientation: Int, direction: Int) = JBUI.scale(16)
    override fun getScrollableBlockIncrement(visibleRect: Rectangle, orientation: Int, direction: Int) =
        if (orientation == SwingConstants.VERTICAL) visibleRect.height else visibleRect.width

    override fun getScrollableTracksViewportWidth() = layout is WrapLayout
    override fun getScrollableTracksViewportHeight() = false
}

/** Scroll pane that zooms with Ctrl/⌘ + wheel or trackpad pinch, and scrolls otherwise. */
private class ZoomableScrollPane(view: JComponent, private val onZoom: (Double) -> Unit) : JBScrollPane(view) {
    init {
        border = JBUI.Borders.empty()
    }

    override fun processMouseWheelEvent(e: MouseWheelEvent) {
        if (e.isControlDown || e.isMetaDown) {
            onZoom(if (e.preciseWheelRotation < 0) 1.1 else 1 / 1.1)
            e.consume()
        } else {
            super.processMouseWheelEvent(e)
        }
    }
}
