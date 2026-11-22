# Security policy

## Supported versions

| Version | Security fixes |
|---|---|
| 1.x (latest minor) | Yes |
| Older 1.x minors | Critical issues only, for 6 months after the next minor |
| 0.x | No |

## Reporting a vulnerability

Please **don't open a public issue**. Report it privately through GitHub:
[Security › Report a vulnerability](https://github.com/NucleusFramework/ComposeSwiftUI/security/advisories/new).

Include the affected version, what an attacker can do, and how to reproduce it. You can expect:

- an acknowledgement within **3 working days**;
- an assessment (accepted or not, severity) within **10 working days**;
- a fix in a patch release, a GitHub Security Advisory and a CVE when it applies, crediting you unless you prefer not.

We ask you to keep the issue private until the advisory is published, and at most 90 days after your report.

## Scope

The runtime library, the Swift renderer, the Gradle plugin and the IDE plugin of this repository. The IDE plugin runs
commands on a remote Mac over SSH for remote builds: issues there are in scope. Vulnerabilities of Apple's frameworks
or of dependencies go to their maintainers; tell us if Compose SwiftUI makes them reachable.

## Verifying releases

Release files and Maven artifacts carry GitHub provenance attestations:

```bash
gh attestation verify compose-swiftui-intellij-plugin-0.10.0-signed.zip --repo NucleusFramework/ComposeSwiftUI
```

Each release also attaches the CycloneDX SBOM of the library (`compose-swiftui-<version>.cdx.json`).
