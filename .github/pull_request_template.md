## What and why

<!-- What changes, and the problem it solves. Link the issue. -->

## Checklist

- [ ] Tests: unit (`./gradlew :runtime:macosArm64Test`), and `scripts/run_e2e.sh` for runtime or renderer changes
- [ ] Public API: `./gradlew :runtime:checkKotlinAbi` passes, or `updateKotlinAbi` is committed and the change is
      explained (a removal or an incompatible change needs a major version, see docs/VERSIONING.md)
- [ ] Renderer: `python3 scripts/check_n1.py` passes (APIs of the newest SDKs behind `#if compiler(>=…)`)
- [ ] Docs: guide updated, `python3 scripts/check_docs.py` passes
- [ ] CHANGELOG.md entry under the next version
