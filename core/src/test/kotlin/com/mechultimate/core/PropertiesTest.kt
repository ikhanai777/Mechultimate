package com.mechultimate.core

import com.mechultimate.core.numerics.Numerics
import com.mechultimate.core.props.IF97
import com.mechultimate.core.props.Psychro
import com.mechultimate.core.units.Kind
import com.mechultimate.core.units.Units
import com.mechultimate.core.units.formatNumber
import org.junit.Assert.assertEquals
import org.junit.Test

class PropertiesTest {
    private fun rel(expected: Double, actual: Double, tol: Double = 1e-6) =
        assertEquals(expected, actual, kotlin.math.abs(expected) * tol)

    // IAPWS-IF97 verification values (Tables 5, 15, 35, 36 of the release).
    @Test fun if97Region1() {
        val a = IF97.region1(3e6, 300.0)
        rel(0.100215168e-2, a.v); rel(0.115331273e3, a.h / 1000); rel(0.392294792, a.s / 1000); rel(0.417301218e1, a.cp / 1000)
        val b = IF97.region1(80e6, 300.0)
        rel(0.971180894e-3, b.v); rel(0.184142828e3, b.h / 1000)
        val c = IF97.region1(3e6, 500.0)
        rel(0.975542239e3, c.h / 1000); rel(0.258041912e1, c.s / 1000)
    }

    @Test fun if97Region2() {
        val a = IF97.region2(3500.0, 300.0)
        rel(0.394913866e2, a.v); rel(0.254991145e4, a.h / 1000); rel(0.852238967e1, a.s / 1000)
        val b = IF97.region2(3500.0, 700.0)
        rel(0.923015898e2, b.v); rel(0.333568375e4, b.h / 1000)
        val c = IF97.region2(30e6, 700.0)
        rel(0.542946619e-2, c.v); rel(0.263149474e4, c.h / 1000); rel(0.517540298e1, c.s / 1000)
    }

    @Test fun if97Region4() {
        rel(0.353658941e-2, IF97.psat(300.0) / 1e6)
        rel(0.263889776e1, IF97.psat(500.0) / 1e6)
        rel(0.123443146e2, IF97.psat(600.0) / 1e6)
        rel(0.372755919e3, IF97.tsat(0.1e6))
        rel(0.453035632e3, IF97.tsat(1e6))
        rel(0.584149488e3, IF97.tsat(10e6))
    }

    @Test fun if97B23() = rel(0.165291643e2, IF97.pB23(623.15) / 1e6)

    @Test fun psychrometricsSeaLevel() {
        // 25 °C, 50 % RH at sea level — ASHRAE chart values.
        val s = Psychro.fromTdbRh(25.0, 0.5, 101_325.0)
        assertEquals(0.009881, s.w, 0.00005)
        assertEquals(13.86, s.tdp, 0.05)
        assertEquals(17.9, s.twb, 0.15)
        assertEquals(50.3, s.h / 1000, 0.2)
        assertEquals(0.858, s.v, 0.002)
    }

    @Test fun psychrometricsRoundTrip() {
        val s = Psychro.fromTdbRh(32.0, 0.4, Psychro.pressureAtAltitude(1500.0))
        val back = Psychro.fromTdbTwb(32.0, s.twb, s.p)
        assertEquals(s.w, back.w, 1e-7)
        val back2 = Psychro.fromTdbTdp(32.0, s.tdp, s.p)
        assertEquals(s.rh, back2.rh, 1e-6)
    }

    @Test fun saturationPressureAt20C() = assertEquals(2338.8, Psychro.pws(20.0), 1.0)

    @Test fun colebrook() {
        assertEquals(0.01851, Numerics.darcyFriction(1e5, 1e-4), 0.0001)
        assertEquals(64.0 / 1000, Numerics.darcyFriction(1000.0, 0.0), 1e-12)
    }

    @Test fun brentFindsRoot() = assertEquals(kotlin.math.sqrt(2.0), Numerics.brent(0.0, 2.0) { it * it - 2 }, 1e-10)

    @Test fun unitConversions() {
        assertEquals(6.894757, Units.convert(Kind.STRESS, 1.0, "psi", "kPa"), 1e-6)
        assertEquals(37.77778, Units.convert(Kind.TEMPERATURE, 100.0, "°F", "°C"), 1e-5)
        assertEquals(212.0, Units.convert(Kind.TEMPERATURE, 373.15, "K", "°F"), 1e-9)
        assertEquals(1.0, Units.convert(Kind.TEMP_DIFF, 1.8, "Δ°F", "Δ°C"), 1e-12)
        assertEquals(14.5038, Units.convert(Kind.PRESSURE_GAUGE, 1.0, "bar(g)", "psig"), 1e-4)
        assertEquals(1.0, Units.convert(Kind.LENGTH, 25.4, "mm", "in"), 1e-12)
        assertEquals(3785.411784, Units.convert(Kind.VOLUME, 1000.0, "gal", "L"), 1e-6)
    }

    @Test fun everyKindDefaultUnitsExist() {
        for (k in Kind.entries) {
            k.unit(k.siDefault); k.unit(k.usDefault)
            assertEquals("SI base unit of $k must have factor 1", 1.0, k.si.factor, 0.0)
        }
    }

    @Test fun numberFormatting() {
        assertEquals("1.213", formatNumber(1.21323))
        assertEquals("1235", formatNumber(1234.6))
        assertEquals("0.001234", formatNumber(0.0012341))
        assertEquals("0", formatNumber(0.0))
    }
}
