# Android Studio's agent and Compose previews

What Android Studio's AI agent (Gemini, and the agents brought by "Bring Your Own Agent") can do with Compose
previews, as of Android Studio Rabbit 2 (build 262, November 2026), and how Compose SwiftUI covers each feature.

Sources: the Android Studio and Android CLI documentation, the public AOSP sources of `tools/adt/idea`
(`compose-designer`, `cli-server`), and the strings of the Gemini plugin of an installed Android Studio. The agents and
their tools are closed source: tool names and parameters below come from the installed plugin, not from published code,
and may change between builds.

## IDE features

| Feature | Entry point | What the agent does | Tools it uses |
|---|---|---|---|
| Generate Compose Preview | Right-click a composable › *AI › Generate Compose Preview*, or the link of an empty preview pane | Writes `@Preview` functions with mock data for the parameters, shown as a diff | analyze file, Gradle build, `render_compose_preview`; fixes and renders again, at most 5 times |
| Generate code from a screenshot | Empty preview pane, or an image attached in the chat | Writes a composable and its preview from a design mock | `render_compose_preview` |
| Match UI to Target Image | Right-click a preview › *AI Actions › Match UI to Target Image* | Splits the target image into elements, compares them with the code and the render, edits until they match (5 rounds) | `render_compose_preview`, Material Symbols search, vector drawables |
| Transform UI / Change UI / Edit Component | Right-click a preview (or a sub-component of it) › *AI Actions › Edit Component* | Applies a natural-language change and verifies it by rendering | `render_compose_preview` |
| Fix UI Check issues | *Fix with AI* in UI Check mode, *AI Actions › Fix all UI check issues* | Lists accessibility, contrast, typography and adaptive-layout issues, asks which to fix, fixes, checks again | `visual_lint_service_tool`, `render_compose_preview` |
| Fix render errors | *Fix with AI* on a preview that fails | Reads the render stack trace, fixes the failing code, renders again | `render_compose_preview` |
| Migrate XML layouts to Compose | Right-click a layout › *AI › Migrate to Compose* | Rewrites the layout in Compose, judges the render against the old screenshot, writes a parity report | `compose_visual_comparison_judge`, `compare_images`, `verify_batch_visual_parity` |
| In-memory prototypes | Agent mode (behind a flag) | Renders throwaway code without touching the project, usually several variants (light/dark, phone/tablet, states) | `show_in_memory_compose_preview` |

## Agent tools

| Tool | Parameters | Result |
|---|---|---|
| `render_compose_preview` | `filePath`, `composableName`, `previewIndex` (0-based, among the variants of multipreviews and `@PreviewParameter`) | PNG and a semantics tree (absolute bounds, accessibility text); the error and stack trace when it fails. Compiles the file only, assuming the rest is built |
| `show_in_memory_compose_preview` | `code` (a whole Kotlin file without `package`), `composableNames`, `contextFilePath` (module and package) | Images and UI hierarchy, without editing the project |
| `visual_lint_service_tool` | Same as `render_compose_preview` | Issues in four categories: accessibility, color contrast, typography, adaptive layout |
| `compose_visual_comparison_judge` | `filePath`, `composableName`, `goldenScreenshotPath` | The differences (position, font, size, color), or that they match |
| `compare_images` | `image1Path`, `image2Path`, `diffOutputPath` | Mismatch %, PSNR, MSE, RMSE, and a diff image in red |
| `verify_batch_visual_parity` | `beforeDirectoryPath`, `afterDirectoryPath`, `reportOutputPath` | Pairs PNGs by name, writes a Markdown report: pairs, matches, mismatches, score per pair |

## For agents outside the IDE

- **`android studio render-compose-preview <path> <composable>`** (Android CLI, needs a running Android Studio):
  writes the PNG, prints its path, `--print-semantics` prints the semantics tree.
- **`android preview list | render`** (Android CLI 1.1, no IDE or device): lists previews by qualified name (custom
  multipreviews included); `render --fqn … --component-tree` writes images and a component tree (bounds, names, source
  locations, semantics), `--daemon` stays warm, `--no-image` for the tree alone.
- **Bring Your Own Agent** (Claude, Codex, other ACP agents): Android Studio hands the agent a local MCP server,
  `ide-tools`, with preview rendering.
- **Skills** (`android/android-cli-plugin`, `testing-setup`): wrap the CLI; screenshot tests across window sizes,
  themes and font scales.

## In Compose SwiftUI

Previews render with real SwiftUI on the iOS simulator. The same features, for any agent:

| Android Studio | Compose SwiftUI |
|---|---|
| `render_compose_preview` | MCP tool `render_swiftui_preview` (IDE); `renderComposeSwiftUIPreviews` writes the image, a component tree and UI checks in `report.md` (command line) |
| Semantics / component tree | `<key>.tree.txt`: every view with its call site, frame and text, nested |
| `show_in_memory_compose_preview` | MCP tool `render_swiftui_preview_code`; scratch previews in `build/composeSwiftUI/scratch` |
| `visual_lint_service_tool`, UI Check | MCP tool `check_swiftui_preview`; UI checks in `results.json` and `report.md`: touch targets, text contrast, small text, overlapping text, views past the edge |
| `compose_visual_comparison_judge` | MCP tool `compare_swiftui_preview_to_image`; `-PcomposeSwiftUI.golden=<png or folder>`: metrics, diff, side-by-side image and the views of the regions that differ |
| `compare_images` | MCP tool `compare_images` |
| `verify_batch_visual_parity` | MCP tool `verify_batch_visual_parity`; a golden folder on the command line |
| `android preview list` | MCP tool `list_swiftui_previews`; Gradle task `listComposeSwiftUIPreviews` |
| Generate preview, Match UI to image, Transform UI, Fix render errors | The agent's own loop on top of these tools: render, look at the image and the tree, edit, render again. Errors come back with the Kotlin exception or the compiler errors and their lines |
