# Enterprise readiness — checklist

Vérifications : `scripts/run_e2e.sh` (app SwiftUI pilotée par l'API d'accessibilité, builds debug et release, mort de
process, os_log, XCTest), `scripts/run_ios_package_e2e.sh [--cocoapods]` (app iOS dans le simulateur),
`./gradlew :runtime:macosArm64Test :runtime:checkKotlinAbi`, `./gradlew -p gradle-plugin test`,
`generator/test_generator.py`, `scripts/check_n1.py`, `verify.py`, `scripts/check_docs.py`. Tout tourne en CI
(`.github/workflows/ci.yaml`).

## P0 — Runtime : crashs et blocages à la première intégration

### Gestion des erreurs
- [x] `try/catch` autour de `SAction.invoke()` / `SValueCallback.invoke()` et des 26 ponts Kotlin appelés depuis Swift (canvas, layout, drop, key press, charts…) — e2e `callbacks`, `bridges`
- [x] Exceptions pendant la recomposition et dans les effets : le host fautif s'arrête et n'affiche rien (message rouge en debug), les autres continuent — e2e `composition`, `effect`, test `compositionExceptionFailsOnlyThatHost`
- [x] `CoroutineExceptionHandler` sur le scope des `SAsyncAction` — e2e `task`
- [x] `println` remplacé par un log `os_log` (subsystem `dev.nucleusframework.composeswiftui`) — e2e « errors reach os_log »
- [x] Hook public `ComposeSwiftUI.onError` — tous les scénarios d'erreur
- [x] Release : vue vide + log au lieu de « Unsupported view kind » / « No native view » — e2e `unsupported` (build release)
- [x] Symbolication des stack traces mixtes Kotlin/Swift — `docs/guide/33-production.md`

### Cycle de vie
- [x] `asyncActionScope` rattaché au host : coroutines annulées au `dispose()` — e2e `taskCancel`
- [x] `LocalLifecycleOwner` (onAppear / onDisappear / scenePhase) — e2e `lifecycle` (`collectAsStateWithLifecycle`, `LifecycleEventEffect`, app masquée/réaffichée)
- [x] `LocalViewModelStoreOwner` officiel — e2e `viewModels` (`koinViewModel()` réel)
- [x] `SavedStateHandle` (`CreationExtras` non vides) — e2e `viewModels`
- [x] `SaveableStateRegistry` racine + `@SceneStorage` (`ComposeView(restorationID:)`) — e2e `saveable` + phase « process death » (nouveau process)

### Qualité interne du runtime
- [x] Tests unitaires du runtime (`appleTest`) : applier, diff des args, transfert des callbacks, lifecycle, erreurs, codec d'état, flush
- [x] Tests XCTest du renderer Swift (`e2e/RendererTests`, via le package Swift généré)
- [x] Snapshot tests du rendu SwiftUI (`e2e/RendererTests/Snapshots`)
- [x] Test de fuites mémoire ARC ↔ GC — a trouvé et corrigé un cycle racine ↔ modèle Swift au `dispose()`

## P0 — CI et stabilité d'API
- [x] Workflow CI sur PR et `main` (runner macOS, Xcode 26) : build, tests, ABI, `check_docs.py`, `typecheck_swift.sh`, e2e macOS + iOS (SPM, CocoaPods) — vert sur GitHub. La régénération des bindings (`verify.py`) demande le SDK 27, absent des runners : elle reste locale
- [x] Validation d'ABI Kotlin (`checkKotlinAbi`, dump `runtime/api`) — une rupture volontaire est bien détectée
- [x] `explicitApi()` sur le runtime
- [x] `@RequiresOptIn` : `@ExperimentalComposeSwiftUIApi`, `@InternalComposeSwiftUIApi`
- [x] Xcode N-1 : APIs SDK 27 derrière `#if compiler(>=6.4)` (générateur + code manuel) — vérifié pour de vrai : la CI compile et lance tout avec Xcode 26.6. Ce test a trouvé des API rétro-portées que la vérification statique ratait ; `generator/n1_missing.py` les déduit des graphes de symboles d'Xcode 26 (artefact CI)

## P1 — Distribution et intégration Xcode
- [x] XCFramework / SPM / CocoaPods — `assembleComposeSwiftUIPackage` ; app iOS construite et lancée dans le simulateur par SPM et par CocoaPods
- [x] Tâche Gradle qui produit un Swift package (XCFramework + renderer de la même version)
- [x] Plus besoin de `ENABLE_USER_SCRIPT_SANDBOXING=NO` avec le package (app e2e : sandboxing YES, aucune phase de script)
- [x] Renderer figé : le package, ou `syncRendererOnBuild = false` — vérifié sur le graphe de tâches Gradle
- [x] Appels `ComposeIntegrations.register…()` générés depuis `integrations.set(…)` — e2e `integrations` (macOS) et app iOS (Charts)
- [x] Nouveaux fichiers du renderer : dossiers synchronisés documentés, avertissement du sync quand le projet Xcode en oublie, inclusion automatique dans le package — tests du plugin

## P1 — Performance
- [x] `flushCompose()` ne fait tourner que les hosts qui ont du travail — test `flushRunsOnlyTheHostsWithPendingWork`
- [x] Cache de `SNode.children` (et du `kind` côté Swift)
- [ ] Réduire le surcoût de mount — **non atteint** : mesuré (bench release macOS, écrans 1, 6, 16, 30, 42 : +8 à +35 %), cinq pistes essayées sans gain mesurable puis retirées (reporter d'environnement allégé, effacement de type par étape, enfant unique sans ForEach, feuilles sans NodeView). Le profil attribue l'essentiel à AttributeGraph et à l'instanciation de métadonnées génériques des chaînes `ModifiedContent` : demande un travail de fond sur le renderer.

## P1 — Défauts fonctionnels
- [x] Environnement initial passé avant la première composition — e2e `environment` (fenêtre sombre) + snapshot sombre
- [x] `LocalDensity` fourni (décorateur généré par le plugin quand Compose UI est présent) — e2e `resources`
- [x] `Int64` / `UInt64` → `Long` (générateur, `Args.long`) — tests du générateur + XCTest 64 bits ; aucune API générée actuelle n'utilisait ces types

## P1 — Testabilité côté utilisateur
- [x] `runComposeSwiftUITest { }` — utilisé par les tests du runtime
- [x] Finders par texte et par `accessibilityIdentifier`
- [x] Guide XCUITest et snapshot testing — `docs/guide/33-production.md` renvoie aux exemples `e2e/` (pilotage AXUIElement, XCTest + snapshots)

## P1 — Versioning et supply chain
- [x] Release 1.0 avec SemVer strict — politique écrite (`docs/VERSIONING.md`) ; 1.0.0 publiée (tag `v1.0.0`)
- [x] Politique de dépréciation — `docs/VERSIONING.md`
- [x] Matrice de compatibilité Kotlin / Compose runtime / Xcode — `docs/VERSIONING.md`
- [x] Une seule source de vérité pour la version : `VERSION` (runtime, plugin Gradle, plugin IntelliJ) ; un tag différent fait échouer le build
- [x] GitHub Actions épinglées par SHA
- [x] Dependabot
- [x] SBOM CycloneDX + attestations de provenance (release, Maven) — SBOM généré localement ; workflows validés par `actionlint`
- [ ] Plugin IntelliJ signé et publié — **signature vérifiée** (`verifyPluginSignature` avec un certificat de test) ; publication Marketplace câblée dans `release.yaml`, en attente des secrets `JETBRAINS_*`

## P2 — Gouvernance et maturité perçue
- [x] `SECURITY.md`
- [x] `CODE_OF_CONDUCT.md`
- [x] `CODEOWNERS`
- [x] Templates d'issue et de PR
- [x] Copyright du LICENSE aligné sur `NucleusFramework`
- [ ] Second mainteneur — `MAINTAINERS.md` créé ; reporté : personne pour l'instant
- [ ] Page d'études de cas — `docs/CASE_STUDIES.md` créée (modèle) ; aucune app en production à citer pour l'instant
- [x] `docs/bench/RESULTS.md` mis en avant en tête du README
