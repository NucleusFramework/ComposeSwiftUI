package dev.nucleusframework.composeswiftui.idea

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtVisitorVoid

/**
 * Flags `@Preview` functions that the Gradle plugin skips (private, nested, missing `@Composable`, parameters
 * without `@PreviewParameter`…), which otherwise just don't show up.
 */
class PreviewInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        val file = holder.file as? KtFile ?: return PsiElementVisitor.EMPTY_VISITOR
        // Only files using the library: `@Preview` of Android or Compose Multiplatform alone is not ours to check.
        if (file.importDirectives.none { it.importPath?.pathStr?.startsWith("dev.nucleusframework.composeswiftui") == true }) {
            return PsiElementVisitor.EMPTY_VISITOR
        }
        return object : KtVisitorVoid() {
            override fun visitNamedFunction(function: KtNamedFunction) {
                if (!function.hasPreviewAnnotation()) return
                val problem = function.previewProblem() ?: return
                val fixes = buildList<LocalQuickFix> {
                    if (function.hasModifier(KtTokens.PRIVATE_KEYWORD)) add(RemovePrivateFix())
                    if (function.annotationEntries.none { it.shortName?.asString() == "Composable" }) add(AddComposableFix())
                }
                holder.registerProblem(function.nameIdentifier ?: function, "$problem: this preview won't be rendered", *fixes.toTypedArray())
            }
        }
    }

    private class RemovePrivateFix : LocalQuickFix {
        override fun getFamilyName() = "Make the preview public"
        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val function = descriptor.psiElement.parent as? KtNamedFunction ?: descriptor.psiElement as? KtNamedFunction ?: return
            val modifier = function.modifierList?.getModifier(KtTokens.PRIVATE_KEYWORD) ?: return
            // The space after the modifier goes with it.
            (modifier.nextSibling as? PsiWhiteSpace)?.delete()
            modifier.delete()
        }
    }

    private class AddComposableFix : LocalQuickFix {
        override fun getFamilyName() = "Add @Composable"
        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val function = descriptor.psiElement.parent as? KtNamedFunction ?: descriptor.psiElement as? KtNamedFunction ?: return
            val file = function.containingKtFile
            val imported = file.importDirectives.any {
                val path = it.importPath?.pathStr
                path == "androidx.compose.runtime.Composable" || path == "androidx.compose.runtime.*"
            }
            val name = if (imported) "Composable" else "androidx.compose.runtime.Composable"
            val factory = KtPsiFactory(project)
            val modifiers = function.modifierList ?: return
            val entry = modifiers.addBefore(factory.createAnnotationEntry("@$name"), modifiers.firstChild)
            modifiers.addAfter(factory.createNewLine(), entry)
        }
    }
}
