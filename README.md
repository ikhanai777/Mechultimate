# MechUltimate

**The ultimate mechanical engineering toolkit for Android.** It works offline and cites the code clause behind each result. It covers ASME, API, ISO/EN and other standards, piping, pressure vessels, machine design, thermofluids, HVAC, materials, manufacturing and design tools.

> Status: **MVP in progress (v0.1.0).** 65 working calculators across 13 modules, offline, with show-work, unit switching, saved projects and PDF calc sheets. See the roadmap in [`docs/06-roadmap-and-qa.md`](docs/06-roadmap-and-qa.md).

## Build

Requirements: JDK 17+ and the Android SDK (platform 35, build-tools 35). Put the SDK path in `local.properties` as `sdk.dir=...`.

```bash
./gradlew :core:test                 # engine + calculator verification tests (pure JVM)
./gradlew :app:testDebugUnitTest     # Robolectric screen tests; screenshots in app/build/screenshots
./gradlew :app:assembleDebug         # app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:assembleRelease       # minified APK, signed with the debug key for sideloading
```

The release build is signed with the debug key so it can be installed for testing. Configure a real release keystore before publishing to Google Play.

## Project layout

| Path | Contents |
|------|----------|
| `core/` | Pure-Kotlin engine: units (`units/`), calculator DSL and runtime (`engine/`), numerics, reference data (`data/`), IAPWS-IF97 steam and ASHRAE psychrometrics (`props/`), calculator definitions (`calcs/`), and the `Catalog` registry with search |
| `core/src/test` | Verification tests: IAPWS-IF97 verification tables, API 520 worked examples, ASHRAE psychrometric values, hand calculations, and a run of every calculator with its defaults |
| `app/` | Jetpack Compose Android app: home, module browser, search, browse by standard, generated calculator screens, unit converter, saved projects, settings and the PDF calc-sheet export |

### Adding a calculator

Add a `calculator("XXX-NNN", "Title", Module.X) { ... }` definition to the relevant file in `core/src/main/kotlin/com/mechultimate/core/calcs/`, add it to that object's `all` list, and add a test with a reference value. The app builds its input form, results, show-work view and PDF report from the definition. No UI code is needed.

## MVP calculators

| Module | Calculators |
|--------|-------------|
| Piping | B31.3 and B31.1 wall thickness with schedule selection, B31.4 thickness, B31.8 design pressure, blank thickness, support span, thermal expansion, expansion loop, B31.3 flexibility exemption, hydrotest, pneumatic test with stored energy, API RP 14E erosional velocity, Darcy–Weisbach and Hazen–Williams pressure drop, water hammer, pipe weight, PCC-1 flange bolt torque |
| Relief | API 520 gas/vapour (critical and subcritical), steam and liquid sizing with API 526 orifice selection; API 521 fire case |
| Vessels | UG-27 cylinder, sphere/hemi head, 2:1 ellipsoidal, torispherical (App. 1-4), UG-34 flat head, UG-99/UG-100 test pressure, vessel volume and partial liquid level |
| Tanks | API 650 one-foot method, API 653 minimum shell thickness |
| Inspection | Corrosion rate and remaining life (API 510/570), ISO 20816-3 vibration severity, ISO 21940-11 balance tolerance |
| Machine design | DE-Goodman shaft, keys, ISO 281 bearing life, bolted joint preload and torque, helical springs, press fits (Lamé), flywheels, fillet welds |
| Structures | Beam cases, Euler/Johnson columns, Mohr's circle (von Mises and Tresca), torsion |
| Fluids | Reynolds number, pump power with motor sizing, NPSHa (IF97 vapour pressure), affinity laws, specific speed, IEC 60534 liquid control valve, orifice flow |
| Thermo | IAPWS-IF97 steam tables, insulated-pipe heat loss, LMTD, ε-NTU |
| HVAC | Psychrometrics, supply airflow, duct sizing and friction |
| Welding & materials | Heat input (ASME IX QW-409.1), carbon equivalent / Pcm |
| Manufacturing | Speeds, feeds and cutting power; tap drill; sheet-metal bend allowance |
| General | Single-DOF vibration and isolation |

Licensed tabular data (ASME II-D allowable stresses, B16.5 P-T ratings) is not bundled. Users enter allowable stresses from their own licensed code copy, per the licensing strategy in `docs/02-standards-coverage.md`.

## Specification index

| # | Document | What it covers |
|---|----------|----------------|
| 1 | [Product Specification](docs/01-product-spec.md) | Vision, users, scope, features, UX, monetization, non-functional requirements |
| 2 | [Standards Coverage](docs/02-standards-coverage.md) | Every code and standard (ASME, API, ISO, EN, AWS, TEMA, ASHRAE…), what the app does with each, and the licensing strategy |
| 3 | [Calculator Catalog](docs/03-calculator-catalog.md) | All 250+ calculators by module, with governing equations and clause references |
| 4 | [Design Tools](docs/04-design-tools.md) | Interactive tools: 2D frame/truss FEA, piping isometric & stress-lite, sketcher, CAD viewer, P&ID, tolerance stack-up, and more |
| 5 | [Technical Architecture](docs/05-technical-architecture.md) | Android stack, module layout, calculation engine, units, data packs, reports, security |
| 6 | [Roadmap & QA](docs/06-roadmap-and-qa.md) | Phased delivery, verification and validation, release criteria |

## At a glance

- **Platform:** Android 8.0+ (API 26). Distributed as a Play Store AAB and a sideloadable universal APK.
- **Stack:** Kotlin, Jetpack Compose (Material 3), a pure-Kotlin calculation engine, Room/SQLite data packs, and OpenGL ES/Filament for 3D.
- **Offline:** every calculator, table and tool works without a network.
- **Traceability:** every result shows the equation, the code clause, the code edition and the intermediate values, and can be exported as a signed PDF calc sheet.
- **Units:** full SI / US Customary / mixed, with dimensional checking inside the engine.

## Disclaimer

MechUltimate is an engineering aid. It does not replace the governing code document or the judgement of a qualified engineer. Results must be checked by a competent person before they are used for design, fabrication or certification.
