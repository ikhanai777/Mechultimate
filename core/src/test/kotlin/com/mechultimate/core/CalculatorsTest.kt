package com.mechultimate.core

import com.mechultimate.core.engine.CalcResult
import com.mechultimate.core.engine.InputValues
import com.mechultimate.core.engine.Status
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculatorsTest {
    private fun run(id: String, numbers: Map<String, Double> = emptyMap(), choices: Map<String, String> = emptyMap()): CalcResult {
        val calc = assertNotNull(Catalog.get(id)).let { Catalog.get(id)!! }
        return calc.compute(InputValues(numbers, choices))
    }

    private fun CalcResult.value(label: String): Double =
        (outputs.firstOrNull { it.label == label }?.value ?: steps.firstOrNull { it.label == label }?.value)
            ?: error("No result '$label' in ${outputs.map { it.label } + steps.map { it.label }}")

    private fun CalcResult.text(label: String): String = outputs.first { it.label == label }.text!!

    private fun near(expected: Double, actual: Double, relTol: Double = 1e-3) =
        assertEquals(expected, actual, kotlin.math.abs(expected) * relTol)

    @Test fun everyCalculatorRunsWithDefaults() {
        for (calc in Catalog.all) {
            val r = calc.compute(calc.defaults())
            assertNull("${calc.id} failed with defaults: ${r.error} ${r.fieldErrors}", r.error)
            assertTrue("${calc.id} has no primary result", r.outputs.any { it.primary })
            for (o in r.outputs) {
                o.value?.let { assertTrue("${calc.id} '${o.label}' not finite", it.isFinite()) }
            }
            assertTrue("${calc.id} has no references", calc.references.isNotEmpty())
        }
    }

    @Test fun catalogSize() = assertTrue("Expected at least 60 calculators, got ${Catalog.all.size}", Catalog.all.size >= 60)

    @Test fun b313WallThicknessAndSchedule() {
        val r = run("PIP-001", mapOf("P" to 2e6, "S" to 137.9e6, "E" to 1.0, "W" to 1.0, "Y" to 0.4, "c" to 1.5e-3, "MT" to 0.125), mapOf("nps" to "6"))
        near(1.21323e-3, r.value("Pressure design thickness t"))
        near(2.71323e-3, r.value("Minimum required thickness t_m"))
        assertTrue(r.text("Selected schedule").startsWith("Sch 10S"))
        assertEquals(Status.PASS, r.overall)
    }

    @Test fun b318DesignPressure() {
        val r = run("PIP-004", mapOf("D" to 0.6096, "t" to 0.0127, "S" to 448e6, "E" to 1.0, "T" to 1.0), mapOf("cls" to "0.72"))
        near(13.44e6, r.value("Design pressure P"))
    }

    @Test fun api520GasExample() {
        // API 520 Part I worked example: W = 24 270 kg/h, P1 = 670 kPa(a), T = 348 K, Z = 0.90, M = 51, k = 1.11 → A = 3699 mm².
        val r = run("PRV-001", mapOf("W" to 24_270.0 / 3600, "Pset" to 670e3 - 101_325.0, "op" to 0.0, "Pb" to 0.0, "T" to 348.0,
            "M" to 51.0, "Z" to 0.90, "k" to 1.11, "Kd" to 0.975, "Kb" to 1.0, "Kc" to 1.0))
        near(3699e-6, r.value("Required effective area A"), 2e-3)
        assertTrue(r.text("API 526 orifice").startsWith("P"))
    }

    @Test fun api520LiquidExample() {
        // API 520 Part I worked example: Q = 6814 L/min, G = 0.9, P1 = 1896 kPa(g), back pressure 345 kPa(g), Kw = 0.97 → A = 3066 mm².
        val r = run("PRV-004", mapOf("Q" to 6814.0 / 60_000, "Pset" to 1896e3, "op" to 0.0, "Pb" to 345e3, "G" to 0.9, "mu" to 1e-3,
            "Kd" to 0.65, "Kw" to 0.97, "Kc" to 1.0))
        near(3066e-6, r.value("Required effective area A"), 2e-3)
    }

    @Test fun ug27Cylinder() {
        val r = run("VES-001", mapOf("P" to 1e6, "D" to 1.5, "S" to 138e6, "E" to 1.0, "ca" to 3e-3, "tn" to 0.012))
        near(5.4803e-3, r.value("Circumferential stress thickness"))
        near(8.4803e-3, r.value("Required thickness incl. corrosion"))
        near(1.63766e6, r.value("MAWP (corroded)"))
    }

    @Test fun ellipsoidalHeadMatchesFormula() {
        val r = run("VES-003", mapOf("P" to 1e6, "D" to 1.5, "S" to 138e6, "E" to 1.0, "ca" to 0.0, "tn" to 0.0))
        near(1e6 * 1.5 / (2 * 138e6 - 0.2e6), r.value("Pressure thickness t"))
    }

    @Test fun torisphericalStandardHeadMFactor() {
        // L/r = 16.67 gives M ≈ 1.77, i.e. t ≈ 0.885·P·L/(S·E − 0.1·P).
        val r = run("VES-004", mapOf("L" to 1.5, "r" to 0.09, "ca" to 0.0))
        near(1.7706, r.value("Factor M"), 1e-3)
    }

    @Test fun api650OneFoot() {
        val r = run("TNK-001", mapOf("D" to 30.0, "H" to 15.0, "G" to 1.0, "Sd" to 160e6, "St" to 171e6, "CA" to 1.5e-3))
        near(15.0056e-3, r.value("Design shell thickness t_d"))
        near(12.6368e-3, r.value("Hydrostatic test thickness t_t"))
    }

    @Test fun vesselVolumeHorizontalHalfFull() {
        val r = run("VES-025", mapOf("D" to 2.0, "L" to 6.0, "h" to 1.0), mapOf("orient" to "h", "head" to "ell"))
        near(0.5, r.value("Fill fraction"), 1e-9)
    }

    @Test fun vesselVolumeVerticalFull() {
        val r = run("VES-025", mapOf("D" to 2.0, "L" to 6.0, "h" to 7.0), mapOf("orient" to "v", "head" to "ell"))
        near(1.0, r.value("Fill fraction"), 1e-9)
    }

    @Test fun bearingLife() {
        val r = run("MCH-005", mapOf("C" to 30_700.0, "Fr" to 3000.0, "Fa" to 0.0, "X" to 1.0, "Y" to 0.0, "n" to 1500 * 2 * Math.PI / 60, "aiso" to 1.0),
            mapOf("type" to "ball", "rel" to "1.0"))
        near(1071.67, r.value("Basic rating life L₁₀"))
        near(11907.4 * 3600, r.value("L₁₀h"))
    }

    @Test fun simplySupportedBeam() {
        val r = run("STR-003", mapOf("L" to 4.0, "w" to 5000.0, "E" to 200e9, "I" to 8.356e-6), mapOf("case" to "ss_udl"))
        near(10_000.0, r.value("Maximum moment"))
        near(0.0099729, r.value("Maximum deflection"))
    }

    @Test fun mohrPrincipalStresses() {
        val r = run("STR-006", mapOf("sx" to 80e6, "sy" to -40e6, "txy" to 50e6))
        near(98.1025e6, r.value("σ₁"))
        near(-58.1025e6, r.value("σ₂"))
    }

    @Test fun rectangularDuctEquivalentDiameter() {
        val r = run("HVC-005", mapOf("a" to 0.5, "b" to 0.3), mapOf("shape" to "rect"))
        near(0.41998, r.value("Equivalent round diameter"))
    }

    @Test fun bendAllowance() {
        val r = run("MFG-003", mapOf("t" to 2e-3, "R" to 2e-3, "ang" to Math.PI / 2, "K" to 0.44, "L1" to 0.05, "L2" to 0.03))
        near(4.5239e-3, r.value("Bend allowance BA"))
        near(76.524e-3, r.value("Flat length"))
    }

    @Test fun steamSaturationAt1MPa() {
        val r = run("THM-001", mapOf("P" to 1e6), mapOf("mode" to "satP"))
        near(453.0356, r.value("Saturation temperature"), 1e-6)
        near(2014.6e3, r.value("Latent heat h_fg"), 2e-3) // steam tables: h_fg ≈ 2014.6 kJ/kg at 1 MPa
    }

    @Test fun steamRegion3IsRejected() {
        val r = run("THM-001", mapOf("P" to 25e6, "T" to 650.0), mapOf("mode" to "pt"))
        assertNotNull(r.error)
    }

    @Test fun validationRejectsOutOfRange() {
        val r = run("PIP-001", mapOf("P" to -1.0))
        assertTrue(r.fieldErrors.containsKey("P"))
        assertEquals(Status.FAIL, r.overall)
    }

    @Test fun searchFindsByClauseAndSynonym() {
        assertEquals("VES-001", Catalog.search("UG-27").first().id)
        assertEquals("PIP-001", Catalog.search("B31.3 wall thickness").first().id)
        assertTrue(Catalog.search("psv gas").any { it.id == "PRV-001" })
        assertTrue(Catalog.search("zzzz").isEmpty())
    }
}
