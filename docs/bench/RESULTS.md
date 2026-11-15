# Benchmark: SwiftUI vs Compose SwiftUI

## iOS simulator: 50 screens

- Pixel-identical (≤0.01% before and after 30 updates): **48 / 50**
- Mount (median per screen, then mean): SwiftUI **24.08 ms**, Compose SwiftUI **26.06 ms** (×1.08)
- Update (state change → laid out): SwiftUI **1.915 ms**, Compose SwiftUI **1.476 ms** (×0.77)

| # | Screen | diff | after updates | mount SwiftUI | mount Compose | update SwiftUI | update Compose |
|---|---|---|---|---|---|---|---|
| 1 | 01 · Profile & settings | 0.0000% | 0.0000% | 25.87 | 26.85 | 1.831 | 1.032 |
| 2 | 02 · Mail inbox | 0.0000% | 0.0000% | 29.56 | 30.39 | 1.383 | 1.049 |
| 3 | 03 · Analytics dashboard | 0.0000% | 0.0000% | 28.30 | 29.63 | 2.185 | 1.483 |
| 4 | 04 · Music player | 0.0000% | 0.0000% | 12.93 | 14.25 | 1.090 | 0.717 |
| 5 | 05 · Weather | 0.0281% | 0.1333% | 16.81 | 19.32 | 1.250 | 0.907 |
| 6 | 06 · Shopping cart | 0.0000% | 0.0000% | 30.26 | 32.14 | 2.336 | 1.663 |
| 7 | 07 · Calendar agenda | 0.0000% | 0.0000% | 18.25 | 21.93 | 1.628 | 1.430 |
| 8 | 08 · Chat conversation | 0.0000% | 0.0000% | 16.03 | 17.21 | 1.244 | 0.696 |
| 9 | 09 · Sign-up form | 0.0000% | 0.0000% | 41.05 | 42.31 | 4.601 | 1.484 |
| 10 | 10 · Preferences | 0.0000% | 0.0000% | 41.24 | 43.44 | 6.229 | 6.710 |
| 11 | 11 · Photo grid | 0.0000% | 0.0000% | 18.20 | 21.30 | 1.432 | 1.614 |
| 12 | 12 · Fitness activity | 0.0000% | 0.0000% | 18.59 | 21.96 | 1.756 | 1.564 |
| 13 | 13 · Stocks watchlist | 0.0000% | 0.0000% | 24.23 | 27.47 | 3.262 | 3.857 |
| 14 | 14 · News feed | 0.0000% | 0.0000% | 15.65 | 17.29 | 0.915 | 0.989 |
| 15 | 15 · File browser | 0.0000% | 0.0000% | 34.16 | 35.76 | 1.847 | 0.832 |
| 16 | 16 · Kanban board | 0.0000% | 0.0000% | 27.05 | 31.04 | 2.469 | 2.359 |
| 17 | 17 · Recipe detail | 0.0000% | 0.0000% | 19.59 | 21.20 | 1.558 | 1.418 |
| 18 | 18 · Banking | 0.0000% | 0.0000% | 26.02 | 27.22 | 1.670 | 1.168 |
| 19 | 19 · Travel booking | 0.0000% | 0.0000% | 24.17 | 25.85 | 1.492 | 1.045 |
| 20 | 20 · Contacts | 0.0000% | 0.2485% | 39.32 | 39.45 | 2.467 | 1.033 |
| 21 | 21 · Notification center | 0.0000% | 0.0000% | 17.47 | 19.53 | 1.244 | 0.778 |
| 22 | 22 · Map overview | 0.0000% | 0.0000% | 14.09 | 15.00 | 1.299 | 1.283 |
| 23 | 23 · Bar chart report | 0.0000% | 0.0000% | 16.29 | 17.79 | 1.462 | 1.201 |
| 24 | 24 · Pricing tiers | 0.0000% | 0.0000% | 18.05 | 19.84 | 2.154 | 2.226 |
| 25 | 25 · Onboarding | 0.0000% | 0.0000% | 11.69 | 11.50 | 2.255 | 1.160 |
| 26 | 26 · Leaderboard | 0.0000% | 0.0000% | 30.47 | 31.96 | 1.979 | 1.719 |
| 27 | 27 · Order tracking | 0.0000% | 0.0000% | 16.65 | 18.71 | 2.464 | 1.817 |
| 28 | 28 · Smart home | 0.0000% | 0.0000% | 22.25 | 23.76 | 1.731 | 1.427 |
| 29 | 29 · Podcast episodes | 0.0000% | 0.0000% | 26.63 | 28.13 | 2.191 | 1.736 |
| 30 | 30 · Pull request review | 0.0000% | 0.0000% | 17.10 | 19.34 | 1.578 | 1.791 |
| 31 | 31 · Social profile | 0.0000% | 0.0000% | 19.25 | 21.28 | 2.506 | 2.519 |
| 32 | 32 · Habit tracker | 0.0000% | 0.0000% | 24.38 | 26.44 | 1.687 | 0.750 |
| 33 | 33 · Expense splitting | 0.0000% | 0.0000% | 17.85 | 20.11 | 1.490 | 1.314 |
| 34 | 34 · Video library | 0.0000% | 0.0000% | 29.38 | 34.14 | 1.136 | 0.832 |
| 35 | 35 · Online course | 0.0000% | 0.0000% | 29.06 | 30.89 | 1.519 | 0.982 |
| 36 | 36 · Hotel detail | 0.0000% | 0.0000% | 20.41 | 21.35 | 1.379 | 0.825 |
| 37 | 37 · Boarding pass | 0.0000% | 0.0000% | 15.28 | 18.47 | 1.492 | 1.263 |
| 38 | 38 · Medication schedule | 0.0000% | 0.0000% | 41.95 | 44.67 | 2.114 | 1.127 |
| 39 | 39 · Analytics table | 0.0000% | 0.0000% | 18.06 | 18.59 | 1.390 | 1.146 |
| 40 | 40 · Restaurant menu | 0.0000% | 0.0000% | 33.00 | 36.33 | 1.455 | 0.515 |
| 41 | 41 · Crypto wallet | 0.0000% | 0.0000% | 29.75 | 32.73 | 2.120 | 2.009 |
| 42 | 42 · Task manager | 0.0000% | 0.0000% | 27.01 | 27.79 | 1.424 | 0.968 |
| 43 | 43 · Event tickets | 0.0000% | 0.0000% | 30.54 | 35.85 | 3.151 | 3.528 |
| 44 | 44 · Job listings | 0.0000% | 0.0000% | 24.99 | 26.60 | 1.633 | 1.052 |
| 45 | 45 · Real-estate listing | 0.0000% | 0.0000% | 17.59 | 18.38 | 1.485 | 1.151 |
| 46 | 46 · Sports scores | 0.0000% | 0.0000% | 25.51 | 28.80 | 1.624 | 0.876 |
| 47 | 47 · Book library | 0.0000% | 0.0000% | 19.28 | 22.35 | 1.536 | 0.671 |
| 48 | 48 · EV dashboard | 0.0000% | 0.0000% | 31.69 | 31.49 | 1.881 | 1.390 |
| 49 | 49 · Sleep tracker | 0.0000% | 0.0000% | 27.49 | 30.35 | 1.883 | 1.125 |
| 50 | 50 · System status | 0.0000% | 0.0000% | 23.73 | 24.78 | 1.857 | 1.581 |

## macOS: 50 screens

- Pixel-identical (≤0.01% before and after 30 updates): **50 / 50**
- Mount (median per screen, then mean): SwiftUI **14.44 ms**, Compose SwiftUI **17.06 ms** (×1.18)
- Update (state change → laid out): SwiftUI **2.108 ms**, Compose SwiftUI **2.071 ms** (×0.98)

| # | Screen | diff | after updates | mount SwiftUI | mount Compose | update SwiftUI | update Compose |
|---|---|---|---|---|---|---|---|
| 1 | 01 · Profile & settings | 0.0000% | 0.0000% | 14.12 | 15.54 | 1.410 | 1.407 |
| 2 | 02 · Mail inbox | 0.0000% | 0.0000% | 14.56 | 16.30 | 1.369 | 1.383 |
| 3 | 03 · Analytics dashboard | 0.0000% | 0.0000% | 17.34 | 18.77 | 2.204 | 2.477 |
| 4 | 04 · Music player | 0.0000% | 0.0000% | 8.74 | 10.15 | 1.151 | 1.093 |
| 5 | 05 · Weather | 0.0000% | 0.0000% | 9.01 | 12.79 | 1.046 | 1.052 |
| 6 | 06 · Shopping cart | 0.0000% | 0.0000% | 22.26 | 25.00 | 1.938 | 1.915 |
| 7 | 07 · Calendar agenda | 0.0000% | 0.0000% | 11.26 | 15.89 | 1.923 | 2.306 |
| 8 | 08 · Chat conversation | 0.0000% | 0.0000% | 7.95 | 9.87 | 1.484 | 1.040 |
| 9 | 09 · Sign-up form | 0.0000% | 0.0000% | 29.63 | 31.20 | 4.235 | 4.589 |
| 10 | 10 · Preferences | 0.0000% | 0.0000% | 35.19 | 37.36 | 8.566 | 9.906 |
| 11 | 11 · Photo grid | 0.0000% | 0.0000% | 7.44 | 11.69 | 0.847 | 1.127 |
| 12 | 12 · Fitness activity | 0.0000% | 0.0000% | 10.72 | 14.20 | 1.765 | 2.142 |
| 13 | 13 · Stocks watchlist | 0.0000% | 0.0000% | 14.68 | 17.77 | 2.960 | 3.595 |
| 14 | 14 · News feed | 0.0000% | 0.0000% | 8.83 | 11.10 | 0.877 | 1.169 |
| 15 | 15 · File browser | 0.0000% | 0.0000% | 13.28 | 15.18 | 1.923 | 0.695 |
| 16 | 16 · Kanban board | 0.0000% | 0.0000% | 16.76 | 20.17 | 1.851 | 2.071 |
| 17 | 17 · Recipe detail | 0.0000% | 0.0000% | 12.20 | 14.23 | 2.145 | 2.381 |
| 18 | 18 · Banking | 0.0000% | 0.0000% | 19.81 | 21.36 | 1.738 | 0.751 |
| 19 | 19 · Travel booking | 0.0000% | 0.0000% | 14.63 | 16.93 | 2.109 | 2.159 |
| 20 | 20 · Contacts | 0.0000% | 0.0000% | 26.13 | 30.26 | 4.074 | 2.552 |
| 21 | 21 · Notification center | 0.0000% | 0.0000% | 8.98 | 11.01 | 1.157 | 0.956 |
| 22 | 22 · Map overview | 0.0000% | 0.0000% | 7.52 | 10.43 | 1.237 | 1.564 |
| 23 | 23 · Bar chart report | 0.0000% | 0.0000% | 9.13 | 10.97 | 2.043 | 2.196 |
| 24 | 24 · Pricing tiers | 0.0000% | 0.0000% | 13.55 | 16.26 | 3.567 | 3.709 |
| 25 | 25 · Onboarding | 0.0000% | 0.0000% | 5.65 | 7.17 | 1.791 | 1.686 |
| 26 | 26 · Leaderboard | 0.0000% | 0.0000% | 16.00 | 19.60 | 1.874 | 1.205 |
| 27 | 27 · Order tracking | 0.0000% | 0.0000% | 9.48 | 12.44 | 2.341 | 2.717 |
| 28 | 28 · Smart home | 0.0000% | 0.0000% | 16.06 | 18.10 | 2.092 | 2.082 |
| 29 | 29 · Podcast episodes | 0.0000% | 0.0000% | 17.35 | 19.63 | 2.623 | 2.011 |
| 30 | 30 · Pull request review | 0.0000% | 0.0000% | 10.08 | 13.86 | 1.569 | 2.433 |
| 31 | 31 · Social profile | 0.0000% | 0.0000% | 14.08 | 16.15 | 3.479 | 4.434 |
| 32 | 32 · Habit tracker | 0.0000% | 0.0000% | 13.45 | 15.52 | 1.527 | 1.083 |
| 33 | 33 · Expense splitting | 0.0000% | 0.0000% | 9.01 | 11.23 | 1.889 | 2.106 |
| 34 | 34 · Video library | 0.0000% | 0.0000% | 14.22 | 19.20 | 1.466 | 1.357 |
| 35 | 35 · Online course | 0.0000% | 0.0000% | 13.56 | 14.92 | 1.712 | 1.100 |
| 36 | 36 · Hotel detail | 0.0000% | 0.0000% | 10.74 | 13.02 | 1.269 | 1.391 |
| 37 | 37 · Boarding pass | 0.0000% | 0.0000% | 9.38 | 13.11 | 1.484 | 1.395 |
| 38 | 38 · Medication schedule | 0.0000% | 0.0000% | 21.46 | 22.71 | 2.469 | 1.505 |
| 39 | 39 · Analytics table | 0.0000% | 0.0000% | 10.56 | 12.22 | 1.888 | 2.083 |
| 40 | 40 · Restaurant menu | 0.0000% | 0.0000% | 16.36 | 18.17 | 1.362 | 0.495 |
| 41 | 41 · Crypto wallet | 0.0000% | 0.0000% | 18.05 | 19.58 | 2.919 | 2.900 |
| 42 | 42 · Task manager | 0.0000% | 0.0000% | 20.45 | 23.43 | 1.772 | 1.275 |
| 43 | 43 · Event tickets | 0.0000% | 0.0000% | 15.48 | 22.30 | 4.690 | 6.127 |
| 44 | 44 · Job listings | 0.0000% | 0.0000% | 17.64 | 20.07 | 1.723 | 1.356 |
| 45 | 45 · Real-estate listing | 0.0000% | 0.0000% | 9.96 | 11.92 | 1.816 | 1.830 |
| 46 | 46 · Sports scores | 0.0000% | 0.0000% | 16.17 | 19.64 | 1.251 | 0.917 |
| 47 | 47 · Book library | 0.0000% | 0.0000% | 12.55 | 16.26 | 0.962 | 0.929 |
| 48 | 48 · EV dashboard | 0.0000% | 0.0000% | 21.03 | 22.01 | 2.114 | 2.258 |
| 49 | 49 · Sleep tracker | 0.0000% | 0.0000% | 17.28 | 20.94 | 1.549 | 0.976 |
| 50 | 50 · System status | 0.0000% | 0.0000% | 12.05 | 15.17 | 2.166 | 1.678 |

## Scenarios: iOS simulator

`scripts/run_scenarios.sh -sizes 1000,10000 [-lazy]`. Plain SwiftUI uses native `@Observable` models (one object per
row); Compose SwiftUI uses an immutable list in one Kotlin state, as a `ViewModel` would. Every case is pixel-identical.
Mount is the median of three mounts after a warm-up; the other numbers are means over 30 one-row changes, 5 full
updates and 60 scrolled pages.

| Scenario | Plain SwiftUI | Compose SwiftUI, `LazyForEach` | Compose SwiftUI, Kotlin loop |
|---|---|---|---|
| 1,000 rows: mount | 23.85 ms | 27.81 ms | 55.90 ms |
| 1,000 rows: one row changes | 0.167 ms | 0.157 ms | 0.562 ms |
| 1,000 rows: every row changes | 1.90 ms | 1.95 ms | 9.02 ms |
| 1,000 rows: scrolling, per page | 9.94 ms | 13.66 ms | 20.20 ms |
| 10,000 rows: mount | 23.48 ms | 28.24 ms | 818.83 ms |
| 10,000 rows: one row changes | 0.160 ms | 0.407 ms | 3.240 ms |
| 10,000 rows: every row changes | 4.00 ms | 4.52 ms | 78.49 ms |
| 10,000 rows: scrolling, per page | 9.72 ms | 13.77 ms | 22.89 ms |
| 60 animated bars: apply one change | 5.12 ms | 7.54 ms | — |
| 60 animated bars: frames / hitches over 10 animations | 254 / 0 | 254 / 0 | — |

## Scenarios: macOS

Same scenarios in a 700×900 window. The frame sampler on macOS is a 120 Hz timer on the main run loop, so its hitch
count is only indicative.

| Scenario | Plain SwiftUI | Compose SwiftUI, `LazyForEach` | Compose SwiftUI, Kotlin loop |
|---|---|---|---|
| 1,000 rows: mount | 26.07 ms | 36.98 ms | 107.42 ms |
| 1,000 rows: one row changes | 0.189 ms | 0.236 ms | 0.691 ms |
| 1,000 rows: every row changes | 6.86 ms | 7.19 ms | 12.63 ms |
| 1,000 rows: scrolling, per page | 8.97 ms | 14.64 ms | 20.47 ms |
| 10,000 rows: mount | 26.46 ms | 37.84 ms | 1250.92 ms |
| 10,000 rows: one row changes | 0.137 ms | 0.468 ms | 3.249 ms |
| 10,000 rows: every row changes | 8.40 ms | 8.95 ms | 87.75 ms |
| 10,000 rows: scrolling, per page | 12.39 ms | 20.95 ms | 40.89 ms |
| 60 animated bars: apply one change | 4.40 ms | 7.09 ms | — |
| 60 animated bars: ticks / late ticks over 10 animations | 506 / 3 | 515 / 8 | — |
