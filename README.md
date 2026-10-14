# MechUltimate

**The ultimate mechanical engineering toolkit for Android.** It works offline and cites the code clause behind each result. It covers ASME, API, ISO/EN and other standards, piping, pressure vessels, machine design, thermofluids, HVAC, materials, manufacturing and design tools.

> Status: **Specification phase.** This repository holds the product and technical specifications. Implementation follows the roadmap in [`docs/06-roadmap-and-qa.md`](docs/06-roadmap-and-qa.md).

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
