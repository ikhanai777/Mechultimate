# 6. Roadmap, Verification and QA

## 6.1 Release phases

| Phase | Timeline (indicative) | Scope | Exit criteria |
|-------|----------------------|-------|---------------|
| **0 — Foundations** | Months 0–3 | Units engine, calc engine + definition schema, design system, data-pack pipeline, report engine v1, CI with the verification harness | 20 calculators running end-to-end from definitions with passing test vectors |
| **1 — MVP (Free + Pro beta)** | Months 3–7 | All **P0** calculators (~110): core piping (B31.1/3/4/8), B16.5 ratings, B36.10/19 data, PRV sizing, VIII-1 shells/heads/nozzles/MDMT/hydrotest, API 650/653 shell, corrosion/remaining life, shafts, bearings, bolts, springs, fits, welds, beams, Mohr, pumps/NPSH, steam tables, LMTD/ε-NTU, psychrometrics, duct sizing, speeds & feeds, sheet metal, unit converter. DT-07, DT-11, DT-12. Projects, PDF reports, search, offline. | Closed beta with 200+ engineers; crash-free ≥ 99.5%; zero open "wrong result" defects |
| **2 — v1.0 Launch** | Months 7–11 | All **P1** calculators; DT-01 FEA, DT-02 isometric, DT-05 viewer, DT-08 pump curves, DT-10 vessel configurator, DT-13, DT-14; licensed ASME packs; batch mode; 5 languages; billing | Verification suite 100% pass; third-party PE review sign-off per module; Play Store launch |
| **3 — v1.x** | Months 11–18 | **P2** calculators; DT-03 pipe stress lite, DT-04 sketcher, DT-06 mechanisms, DT-09 P&ID, DT-15, DT-16; team sync; Enterprise (SSO, MDM, libraries); AI assistant; field tools; FE/PE learning bank | Enterprise pilot with 3 companies |
| **4 — v2** | Months 18+ | **P3** items; KMP iOS/desktop/web; cloud solvers (3D FEA/CFD-lite); DEXPI and PCF round-trip; UHX tubesheets; WRC 107/537 full; nonlinear pipe supports | — |

## 6.2 Team (indicative, Phase 1–2)

- 1 product lead (mechanical engineer).
- 4 Android/Kotlin engineers (UI, engine, design tools, data/reporting).
- 1 native/C++ engineer (CoolProp, OpenCASCADE, performance).
- 3 domain engineers (piping/vessels, rotating/machine design, thermal/HVAC) who author definitions, test vectors and learn content.
- 1 licensed PE reviewer (contract) per discipline for verification sign-off.
- 1 QA automation engineer and 1 UX designer.

## 6.3 Verification and validation (V&V)

Engineering correctness is the product's core promise. Every calculator passes the following before release.

1. **Definition review.** Two-person rule: the author (domain engineer) and an independent checker verify the equation, symbols, units, applicability limits and clause reference against the **code text of the cited edition**.
2. **Test vectors.** At least 5 per calculator:
   - Code/standard worked examples where published (e.g., ASME PTB-4 *VIII-1 Example Problem Manual*, ASME PTB-3 *VIII-2 Example Problem Manual*, API 520 examples, AGMA/ISO examples, Crane TP-410 examples).
   - Textbook problems (Shigley, Roark, Incropera, Crane, Megyesy *Pressure Vessel Handbook*, Kellogg).
   - Boundary cases (applicability limits, zero/negative inputs, unit extremes).
   - Cross-checks against independent tools (validated spreadsheets, commercial software outputs where licensed).
3. **Tolerance.** Results match the reference within 0.1% relative, or within the reference's stated precision. Table interpolation must match the code's interpolation rule exactly.
4. **Property-based tests.** Monotonicity (e.g., thickness increases with P), unit-system invariance (SI vs US give identical SI-normalized results), and inverse consistency (solve-for(P) ∘ solve-for(t) = identity).
5. **Data pack audits.** Every licensed or public table is double-entered or machine-extracted and diffed, and 10% of rows are spot-checked against the source by a second engineer. Checksums are locked per release.
6. **FEA benchmarks.** Textbook beam/frame/truss closed-form comparisons plus NAFEMS-style linear benchmarks (static, modal, buckling).
7. **Report snapshot tests.** Golden PDF image diffs for every calculator.
8. **PE sign-off.** A licensed PE in each discipline reviews and signs the module verification report before a module leaves beta. Verification reports are published in-app (Settings → Verification) for transparency.

## 6.4 Testing strategy (software)

| Level | Tooling | Coverage target |
|-------|---------|-----------------|
| Unit (engine, units, numerics) | Kotlin test, Kotest property testing | ≥ 90% lines in `core:*` |
| Calculator definition tests | Custom harness running `verification/*.yaml` | 100% of calculators |
| UI | Compose UI tests, screenshot tests (Paparazzi/Roborazzi) | All screens, light/dark, phone/tablet, 200% font |
| Integration | Room migrations, pack mounting, report generation | All migrations |
| Instrumented / device | Firebase Test Lab matrix (API 26, 29, 31, 34, latest; low-end and flagship; foldable) | Each release |
| Performance | Macrobenchmark, Baseline Profiles | Budgets in Architecture §5.9 |
| Accessibility | Accessibility Test Framework, manual TalkBack pass | Each release |
| Security | MobSF static scan, dependency audit (OWASP), pen test before v1.0 | Each release |

## 6.5 Release and maintenance

- Release trains every 4 weeks, with hotfixes as needed. Any wrong-result defect is **Severity 1**: fix plus an in-app notice to affected users (projects containing an affected calculator are flagged for re-validation).
- Code edition watch: track publisher release cycles (ASME BPVC biennial, odd years; B31 ~2-year cycles; API on individual schedules) and deliver edition updates within 90 days of publication.
- A public changelog includes a "Results-affecting changes" section.
- Engine and definitions are versioned with semver. Every saved revision stores the versions it was computed with (reproducibility).

## 6.6 Success metrics (KPIs)

| Metric | Year-1 target |
|--------|---------------|
| Installs | 500k |
| Monthly active users | 120k |
| Free → Pro conversion | ≥ 4% |
| Play rating | ≥ 4.6 |
| Crash-free sessions | ≥ 99.8% |
| Wrong-result defects reaching production | 0 Sev-1 open > 72 h |
| Calculators per active user / month | ≥ 8 |
| Reports exported / month | 50k |

## 6.7 Key risks and mitigations

| Risk | Impact | Mitigation |
|------|--------|-----------|
| Standards licensing cost or refusal | Cannot ship allowable stress or rating tables | Ship equations + public data; BYO-license PDF linking; user-import tables; negotiate reseller data license early |
| Calculation error causes harm or liability | Severe | V&V process, PE sign-off, clear disclaimers, professional liability insurance, transparent "show work" |
| Scope creep (250+ calcs + 16 tools) | Delays | Declarative calculators, strict P0–P3 prioritization, discipline-pack releases |
| APK size | Install friction | On-demand packs, ABI splits, R8 |
| Native library complexity (OCCT, CoolProp) | Crashes, size | Isolate in dynamic feature modules; crash monitoring; pure-Kotlin fallbacks where feasible |
| Edition churn | Stale results | Edition model, project-locked editions, change notices |
