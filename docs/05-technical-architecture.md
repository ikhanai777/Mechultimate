# 5. Technical Architecture

## 5.1 Technology stack

| Layer | Choice | Rationale |
|-------|--------|-----------|
| Language | Kotlin 2.x (K2), Kotlin Multiplatform for the engine | Engine shared with future iOS/desktop/web targets |
| UI | Jetpack Compose + Material 3, adaptive layouts (`material3-adaptive`) | Modern, declarative; list-detail on tablets/foldables |
| Architecture | MVI/UDF (ViewModel + StateFlow), Clean Architecture layers | Testable, predictable state for live-updating calcs |
| DI | Hilt (app), Koin or manual DI in KMP modules | Standard Android DI |
| Async | Kotlin Coroutines + Flow | Cancellable calcs, debounced inputs |
| Persistence | Room (SQLite) + SQLCipher for projects; read-only SQLite for data packs; DataStore for prefs | Offline-first, encrypted user data |
| Search | SQLite FTS5 with a synonyms table | Fast offline fuzzy search |
| Math rendering | KaTeX in a lightweight WebView **or** a native Compose LaTeX renderer (e.g., `jlatexmath` port) | "Show work" equations |
| Charts | Vico (Compose charts) + custom Canvas for engineering diagrams (Moody, psychro, Mohr) | Performance and control |
| 2D canvas | Compose Canvas; custom scene graph for DT tools | Touch-optimized editing |
| 3D | Google Filament (PBR, glTF) + OpenCASCADE (via JNI/NDK) for STEP/IGES tessellation | Mature, Android-native |
| Numerics | Pure Kotlin numerics (root finding, ODE, sparse linear algebra); EJML where JVM-only | No server dependency |
| Thermo properties | CoolProp (C++ via NDK, MIT license); IAPWS-IF97 native Kotlin implementation | Accuracy; licensing-compatible |
| PDF reports | Android `PdfDocument` + custom layout engine, or iText 7 / OpenPDF (license-checked) | Signed, paginated calc sheets |
| Spreadsheet export | Apache POI (lite) or FastExcel | XLSX with formulas |
| Sync (Pro/Ent) | Kotlin backend (Ktor) + PostgreSQL; CRDT-style per-calc revisions; E2E encryption option | Team sharing |
| Auth | Firebase Auth / Credential Manager (passkeys); SAML/OIDC for Enterprise | Modern sign-in |
| Billing | Google Play Billing v7+; license server for APK/Enterprise | Monetization |
| Delivery | AAB + Play Asset Delivery (on-demand packs); universal APK with in-app pack downloader (signed packs) | Size budget |
| Crash/perf | Firebase Crashlytics + Performance (opt-in), or Sentry | Reliability metrics |
| CI/CD | GitHub Actions: build, unit tests, verification suite, lint (ktlint/detekt), instrumented tests on Firebase Test Lab, signed release | Quality gate |

## 5.2 Module structure (Gradle)

```
mechultimate/
├── app/                          # Android application, navigation, DI wiring
├── core/
│   ├── units/            (KMP)   # Quantity types, dimensional analysis, unit catalog
│   ├── engine/           (KMP)   # Calculator runtime, expression evaluator, solvers
│   ├── numerics/         (KMP)   # Root finding, interpolation, ODE, linear algebra, FEA kernel
│   ├── properties/       (KMP+NDK) # IF97, CoolProp bridge, psychrometrics, gas mixtures
│   ├── data/                     # Room DBs, data pack loader, repositories
│   ├── report/                   # PDF/XLSX/DOCX/JSON generators
│   ├── designsystem/             # Compose theme, components, numeric keypad, unit field
│   └── common/                   # Utilities, result types, logging
├── calc-defs/                    # Declarative calculator definitions (YAML/Kotlin DSL) + test vectors
│   ├── piping/  vessels/  tanks/  relief/  machine/  structures/
│   ├── fluids/  thermo/  hvac/  materials/  welding/  manufacturing/  general/
├── feature/
│   ├── home/  search/  calculator/  standards/  databases/  projects/
│   ├── reports/  converter/  learn/  assistant/  fieldtools/  settings/
├── designtools/
│   ├── fea2d/  isometric/  pipestress/  sketcher/  cadviewer/  mechanism/
│   ├── stackup/  pumpcurve/  pid/  vesselconfig/  mohr/  psychro/  thermodiagram/  gdt/
├── datapacks/                    # Build tooling to compile CSV/JSON sources → signed SQLite packs
└── verification/                 # Golden test vectors, benchmark problems, report snapshot tests
```

## 5.3 Units engine (`core:units`)

- `Quantity<D: Dimension>`: a value stored internally in SI base units with a compile-time dimension type for common quantities (Length, Pressure, Stress, Temperature, …) plus a runtime dimension vector `[L, M, T, Θ, I, N, J]` for dynamic expressions.
- Strict separation of **absolute temperature** vs **temperature difference**, and **gauge** vs **absolute pressure** (`PressureGauge` and `PressureAbs` types; conversion requires an atmospheric reference).
- The unit catalog is defined in data (JSON) with factor/offset, aliases, symbols and localization keys. Covers 1,000+ units.
- Engine guarantees: every calculator equation is dimension-checked at definition-load time (tests) and at runtime in debug builds. Mismatches fail tests.
- Formatting: significant figures vs fixed decimals, engineering notation, locale decimal separator, and code-rounding rules (e.g., round thickness **up** to the next 0.1 mm or the available schedule).

## 5.4 Calculation engine (`core:engine`) and declarative calculators

Every calculator is a **definition** plus optional custom Kotlin functions. The UI is generated from the definition. This keeps 250+ calculators maintainable.

```yaml
id: PIP-001
title: Pipe wall thickness — ASME B31.3
module: piping
standards:
  - { code: ASME B31.3, editions: [2016, 2018, 2020, 2022, 2024], clause: "304.1.2", equation: "3a" }
inputs:
  - { id: P,  label: Internal design gage pressure, quantity: PressureGauge, default: "10 barg", min: 0 }
  - { id: D,  label: Outside diameter, quantity: Length, lookup: pipe.od(nps), default: "168.3 mm" }
  - { id: S,  label: Allowable stress at design T, quantity: Stress, lookup: material.allowable(spec, grade, T, code=B31.3) }
  - { id: E,  label: Quality factor, quantity: Dimensionless, lookup: table_302_3_4(product_form), range: [0.6, 1.0] }
  - { id: W,  label: Weld strength reduction factor, quantity: Dimensionless, default: 1.0 }
  - { id: Y,  label: Coefficient Y, quantity: Dimensionless, lookup: table_304_1_1(material_class, T) }
  - { id: c,  label: Corrosion + mechanical allowance, quantity: Length, default: "3 mm" }
  - { id: MT, label: Mill under-tolerance, quantity: Ratio, default: "12.5 %" }
steps:
  - { id: t,   expr: "P*D / (2*(S*E*W + P*Y))", ref: "Eq. (3a)" }
  - { id: tm,  expr: "t + c", ref: "304.1.1(a)" }
  - { id: tnom_req, expr: "tm / (1 - MT)" }
checks:
  - { id: thinwall, expr: "t < D/6", fail: "t ≥ D/6 — see 304.1.2(b); special consideration required", severity: error }
  - { id: pressure_ratio, expr: "P/(S*E) <= 0.385", severity: warning }
outputs:
  - { id: tm, primary: true, round: up }
  - { id: tnom_req }
  - { id: schedule, fn: selectSchedule(nps, tnom_req) }
solveFor: [P, t]          # inverse modes; engine uses closed form or Brent's method
tests: verification/PIP-001.yaml
learn: learn/PIP-001.md
```

Engine features:
- An expression language (safe, sandboxed, unit-aware) with math functions, conditionals, table lookups and interpolation.
- Dependency graph (DAG) of steps, so only affected nodes are recomputed when an input changes.
- Inverse solving: symbolic inversion where registered, otherwise bracketed root-finding (Brent) with robust bracket search.
- An iteration framework for implicit methods (Colebrook, API 650 variable design point, flange design).
- Trace capture: every step records inputs, substituted expression, result and reference, which drives "Show work" and reports.
- Edition variants: `steps` can be overridden per edition (`edition: ">=2016"`).
- Custom Kotlin hooks for complex procedures (FEA, Zick, Bell–Delaware), with the same trace API.

## 5.5 Data model (user data)

```
Project(id, name, client, jobNo, location, unitSystemId, codeEditions{}, createdAt, updatedAt, ownerId, teamId?)
 └─ Folder(id, projectId, name, parentId?)
     └─ CalcDocument(id, folderId, calculatorId, title, tag, currentRevId)
         └─ Revision(id, calcId, revLabel, inputs JSON, outputs JSON, trace JSON,
                     engineVersion, defVersion, dataPackVersions{}, status,
                     originator, checker, approver, timestamps, hash)
DesignModel(id, projectId, toolId, model JSON/binary, revisions…)
Attachment(id, ownerId, type, uri, sha256)
CustomMaterial / CustomComponent / CustomFluid (team-scoped libraries)
```

- Revisions are immutable. A revision's `hash` (SHA-256 over canonical JSON + versions) is printed on the report and embedded in the QR code for integrity verification.
- Recalculation of an old revision with a new engine is allowed and creates a new draft revision. A diff of the outputs is shown.

## 5.6 Data packs

| Pack | Contents | Tier | Approx. size |
|------|----------|------|-------------:|
| core | Units, constants, pipe dims (B36.10/19), threads, basic materials, IF97 | Free | 6 MB |
| piping-std | B16.x dimensions, fittings, gaskets, bolting | Pro | 8 MB |
| asme-allowables | II-D / B31.1 / B31.3 allowable stresses, external pressure charts | Licensed | 15 MB |
| flange-ratings | B16.5 / B16.47 / B16.34 / EN 1092-1 P-T tables | Licensed / Pro | 2 MB |
| structural | AISC, EN, IS sections | Pro | 4 MB |
| fluids | CoolProp fluid files, gas mixtures | Pro | 12 MB |
| bearings | ISO dimension series + optional vendor catalogs | Pro | 5–40 MB |
| hvac-climate | ASHRAE design climate data | Licensed | 20 MB |
| learn-media | Diagrams, animations, worked examples | Free | 30 MB |
| cad-runtime | OpenCASCADE native libs | Pro | 25 MB |

Every record carries a provenance block:

```json
{ "source": "ASME B31.3", "edition": "2022", "table": "A-1", "row": "A106 Gr B",
  "license_tier": "licensed", "verified_by": "PE-0012", "verified_on": "2026-05-02",
  "checksum": "sha256:…" }
```

Packs are SQLite files signed with Ed25519. The app verifies the signature before mounting. Licensed packs are additionally encrypted (AES-256-GCM) with keys delivered per entitlement.

## 5.7 Reports (`core:report`)

- A template engine composes pages from blocks: header/title block, project info, design basis, inputs table, step trace (equations rendered to vector), results/checks with status badges, charts, references, assumptions, revision table, and sign-off block.
- Templates come in company-brandable variants (logo, colors, fonts, numbering scheme such as `CALC-{job}-{disc}-{seq}`).
- Output: PDF/A-2b for archival, plus optional PAdES digital signature using a user certificate from the Android KeyChain (Enterprise).
- Snapshot tests render each calculator's report and compare it against golden images.

## 5.8 Security and privacy

- User data is encrypted at rest (SQLCipher; key wrapped by an Android Keystore key, StrongBox when available).
- Biometric lock is optional, per project or app-wide.
- Network: TLS 1.3, certificate pinning for the sync and license APIs.
- Play Integrity API for licensed pack entitlement. Root/tamper detection is advisory only (no lock-out for offline use).
- The AI assistant sends only the user's prompt plus selected, consented calc context. It is disabled by default for Enterprise until an admin enables it.
- No third-party ad SDKs. Analytics are opt-in and exclude all calc inputs and outputs.
- Android Enterprise: managed configurations (disable AI, force editions, disable export to personal apps), work profile support.

## 5.9 Performance and size budgets

| Budget | Target |
|--------|--------|
| Base download | ≤ 40 MB (R8 full mode, resource shrinking, native libs split per ABI) |
| Cold start | ≤ 1.5 s (Baseline Profiles, App Startup, lazy pack mounting) |
| Calculator open | ≤ 300 ms |
| Recompute on input change | ≤ 50 ms closed form; ≤ 500 ms iterative |
| FEA 2D (500 elements) | ≤ 2 s |
| Monte Carlo 10⁶ samples | ≤ 3 s (vectorized, multi-core coroutines) |
| Memory | ≤ 300 MB peak excluding 3D models |
| Frame time | 60 fps target in all scrolling and canvas interactions (≤ 16 ms), checked with Macrobenchmark |

## 5.10 Extensibility

- **User calculators (Pro):** build custom calculators in-app with the same definition schema (form builder + expression editor). Shareable as `.mucalc` files (signed by the author, sandboxed evaluator, no native code).
- **Company libraries (Enterprise):** distribute standard calcs, materials, pipe classes and templates to all seats.
- **Deep links:** `mechultimate://calc/PIP-001?P=10barg&D=168.3mm`, for integration with other apps and QR codes on equipment.
- **Android intents / App Actions:** "Hey Google, open flange rating in MechUltimate".
