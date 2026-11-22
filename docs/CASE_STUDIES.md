# Who uses Compose SwiftUI

Apps and teams that ship Compose SwiftUI screens. Using it in production? Open a pull request that adds a row (or an
issue if you'd rather we write it): what the app does, which screens are Compose SwiftUI, and anything you learned —
what worked, what you had to work around. Numbers (share of screens, crash-free rate, binary size) are welcome.

| App | Platforms | What runs on Compose SwiftUI | Since |
|---|---|---|---|
| *Your app here* | | | |

## What to include in a case study

- The starting point: an existing KMP app (Compose Multiplatform, native SwiftUI over shared view models…) or a new one.
- How the screens are split between Compose SwiftUI and Swift, and how the app integrates it (Gradle build phase,
  Swift package, CocoaPods).
- Results: development time, crash reports (`ComposeSwiftUI.onError`), performance against what it replaced.
- Gaps you hit, with links to the issues.
