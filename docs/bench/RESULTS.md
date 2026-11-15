# Benchmark: SwiftUI vs Compose SwiftUI

## iOS simulator — 50 screens

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

## macOS — 50 screens

- Pixel-identical (≤0.01% before and after 30 updates): **50 / 50**
- Mount (median per screen, then mean): SwiftUI **15.02 ms**, Compose SwiftUI **17.16 ms** (×1.14)
- Update (state change → laid out): SwiftUI **2.153 ms**, Compose SwiftUI **2.126 ms** (×0.99)

| # | Screen | diff | after updates | mount SwiftUI | mount Compose | update SwiftUI | update Compose |
|---|---|---|---|---|---|---|---|
| 1 | 01 · Profile & settings | 0.0000% | 0.0000% | 14.26 | 14.68 | 1.322 | 1.046 |
| 2 | 02 · Mail inbox | 0.0000% | 0.0000% | 15.27 | 16.34 | 1.367 | 1.316 |
| 3 | 03 · Analytics dashboard | 0.0000% | 0.0000% | 16.45 | 19.25 | 2.499 | 2.944 |
| 4 | 04 · Music player | 0.0000% | 0.0000% | 8.56 | 11.01 | 1.385 | 1.170 |
| 5 | 05 · Weather | 0.0000% | 0.0000% | 9.64 | 11.86 | 1.083 | 1.111 |
| 6 | 06 · Shopping cart | 0.0000% | 0.0000% | 27.08 | 28.24 | 2.063 | 2.091 |
| 7 | 07 · Calendar agenda | 0.0000% | 0.0000% | 11.58 | 15.42 | 2.044 | 2.089 |
| 8 | 08 · Chat conversation | 0.0000% | 0.0000% | 8.33 | 9.63 | 1.188 | 0.872 |
| 9 | 09 · Sign-up form | 0.0000% | 0.0000% | 30.06 | 30.28 | 4.595 | 4.842 |
| 10 | 10 · Preferences | 0.0000% | 0.0000% | 34.06 | 35.78 | 8.726 | 9.968 |
| 11 | 11 · Photo grid | 0.0000% | 0.0000% | 8.39 | 12.02 | 0.898 | 1.129 |
| 12 | 12 · Fitness activity | 0.0000% | 0.0000% | 11.08 | 14.61 | 1.843 | 2.289 |
| 13 | 13 · Stocks watchlist | 0.0000% | 0.0000% | 15.37 | 17.52 | 2.952 | 3.813 |
| 14 | 14 · News feed | 0.0000% | 0.0000% | 10.46 | 11.83 | 0.751 | 1.097 |
| 15 | 15 · File browser | 0.0000% | 0.0000% | 13.95 | 15.09 | 1.898 | 0.793 |
| 16 | 16 · Kanban board | 0.0000% | 0.0000% | 16.19 | 20.06 | 2.024 | 2.262 |
| 17 | 17 · Recipe detail | 0.0000% | 0.0000% | 12.59 | 14.53 | 1.913 | 2.554 |
| 18 | 18 · Banking | 0.0000% | 0.0000% | 19.47 | 21.36 | 1.836 | 1.070 |
| 19 | 19 · Travel booking | 0.0000% | 0.0000% | 13.92 | 17.45 | 2.156 | 2.383 |
| 20 | 20 · Contacts | 0.0000% | 0.0000% | 26.31 | 28.79 | 4.353 | 2.342 |
| 21 | 21 · Notification center | 0.0000% | 0.0000% | 8.58 | 11.00 | 1.155 | 1.173 |
| 22 | 22 · Map overview | 0.0000% | 0.0000% | 10.08 | 11.07 | 1.352 | 1.628 |
| 23 | 23 · Bar chart report | 0.0000% | 0.0000% | 9.81 | 12.63 | 2.182 | 1.750 |
| 24 | 24 · Pricing tiers | 0.0000% | 0.0000% | 13.80 | 15.49 | 3.453 | 4.199 |
| 25 | 25 · Onboarding | 0.0000% | 0.0000% | 8.30 | 9.10 | 1.819 | 1.418 |
| 26 | 26 · Leaderboard | 0.0000% | 0.0000% | 18.16 | 21.78 | 1.890 | 1.449 |
| 27 | 27 · Order tracking | 0.0000% | 0.0000% | 9.85 | 13.17 | 1.996 | 2.810 |
| 28 | 28 · Smart home | 0.0000% | 0.0000% | 15.74 | 17.68 | 2.210 | 2.239 |
| 29 | 29 · Podcast episodes | 0.0000% | 0.0000% | 17.82 | 19.89 | 2.163 | 1.960 |
| 30 | 30 · Pull request review | 0.0000% | 0.0000% | 10.36 | 13.31 | 2.359 | 2.250 |
| 31 | 31 · Social profile | 0.0000% | 0.0000% | 13.93 | 16.53 | 3.765 | 4.863 |
| 32 | 32 · Habit tracker | 0.0000% | 0.0000% | 13.68 | 16.55 | 1.355 | 1.014 |
| 33 | 33 · Expense splitting | 0.0000% | 0.0000% | 11.19 | 13.39 | 1.983 | 1.641 |
| 34 | 34 · Video library | 0.0000% | 0.0000% | 13.97 | 17.95 | 1.292 | 1.305 |
| 35 | 35 · Online course | 0.0000% | 0.0000% | 13.93 | 15.46 | 1.994 | 0.929 |
| 36 | 36 · Hotel detail | 0.0000% | 0.0000% | 10.92 | 13.49 | 1.221 | 1.023 |
| 37 | 37 · Boarding pass | 0.0000% | 0.0000% | 10.09 | 12.63 | 1.358 | 1.392 |
| 38 | 38 · Medication schedule | 0.0000% | 0.0000% | 21.32 | 22.61 | 2.212 | 1.167 |
| 39 | 39 · Analytics table | 0.0000% | 0.0000% | 10.67 | 11.78 | 1.648 | 2.163 |
| 40 | 40 · Restaurant menu | 0.0000% | 0.0000% | 17.12 | 18.16 | 1.553 | 0.518 |
| 41 | 41 · Crypto wallet | 0.0000% | 0.0000% | 18.12 | 19.18 | 3.159 | 2.791 |
| 42 | 42 · Task manager | 0.0000% | 0.0000% | 20.63 | 21.28 | 1.857 | 1.407 |
| 43 | 43 · Event tickets | 0.0000% | 0.0000% | 17.74 | 23.13 | 6.056 | 7.772 |
| 44 | 44 · Job listings | 0.0000% | 0.0000% | 18.22 | 20.43 | 1.359 | 1.466 |
| 45 | 45 · Real-estate listing | 0.0000% | 0.0000% | 10.46 | 11.22 | 1.852 | 1.505 |
| 46 | 46 · Sports scores | 0.0000% | 0.0000% | 17.07 | 19.62 | 1.499 | 0.796 |
| 47 | 47 · Book library | 0.0000% | 0.0000% | 12.10 | 15.91 | 0.882 | 0.874 |
| 48 | 48 · EV dashboard | 0.0000% | 0.0000% | 24.06 | 23.50 | 1.908 | 2.162 |
| 49 | 49 · Sleep tracker | 0.0000% | 0.0000% | 18.24 | 19.58 | 1.278 | 1.096 |
| 50 | 50 · System status | 0.0000% | 0.0000% | 11.91 | 14.89 | 1.927 | 2.381 |

## Scenarios — iOS simulator

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

macOS results are not included in this run: the macOS 50-screen numbers above are from the previous release, and the scenarios were measured on iOS only.
