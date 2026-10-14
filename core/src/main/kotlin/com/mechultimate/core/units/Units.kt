package com.mechultimate.core.units

/** Standard atmosphere, used to convert between gauge and absolute pressure. */
const val ATM_PA = 101_325.0
const val G0 = 9.80665

enum class UnitSystem(val label: String) { SI("SI (metric)"), US("US Customary") }

/**
 * A unit of measure. A value expressed in this unit converts to SI as `si = value * factor + offset`.
 */
data class EngUnit(val symbol: String, val name: String, val factor: Double, val offset: Double = 0.0) {
    fun toSi(value: Double) = value * factor + offset
    fun fromSi(si: Double) = (si - offset) / factor
}

/**
 * A physical quantity kind. Values of a kind are always stored and computed in its SI base unit
 * (the first unit in [units]); other units are for input and display only.
 *
 * Gauge pressure, absolute pressure and pressure difference are separate kinds, as are absolute
 * temperature and temperature difference, so they cannot be mixed up by accident.
 */
enum class Kind(
    val label: String,
    val units: List<EngUnit>,
    val siDefault: String,
    val usDefault: String,
) {
    DIMENSIONLESS("Dimensionless", listOf(EngUnit("–", "dimensionless", 1.0)), "–", "–"),
    RATIO("Ratio", listOf(EngUnit("–", "fraction", 1.0), EngUnit("%", "percent", 0.01)), "%", "%"),
    LENGTH(
        "Length",
        listOf(
            EngUnit("m", "metre", 1.0), EngUnit("mm", "millimetre", 1e-3), EngUnit("cm", "centimetre", 1e-2),
            EngUnit("µm", "micrometre", 1e-6), EngUnit("km", "kilometre", 1e3), EngUnit("in", "inch", 0.0254),
            EngUnit("ft", "foot", 0.3048), EngUnit("yd", "yard", 0.9144), EngUnit("mi", "mile", 1609.344),
            EngUnit("mil", "thou", 2.54e-5),
        ), "mm", "in",
    ),
    AREA(
        "Area",
        listOf(
            EngUnit("m²", "square metre", 1.0), EngUnit("mm²", "square millimetre", 1e-6),
            EngUnit("cm²", "square centimetre", 1e-4), EngUnit("in²", "square inch", 6.4516e-4),
            EngUnit("ft²", "square foot", 0.09290304),
        ), "mm²", "in²",
    ),
    VOLUME(
        "Volume",
        listOf(
            EngUnit("m³", "cubic metre", 1.0), EngUnit("L", "litre", 1e-3), EngUnit("mL", "millilitre", 1e-6),
            EngUnit("in³", "cubic inch", 1.6387064e-5), EngUnit("ft³", "cubic foot", 0.028316846592),
            EngUnit("gal", "US gallon", 3.785411784e-3), EngUnit("bbl", "oil barrel", 0.158987294928),
        ), "m³", "ft³",
    ),
    MASS(
        "Mass",
        listOf(
            EngUnit("kg", "kilogram", 1.0), EngUnit("g", "gram", 1e-3), EngUnit("t", "tonne", 1e3),
            EngUnit("lb", "pound", 0.45359237), EngUnit("ton(US)", "short ton", 907.18474),
        ), "kg", "lb",
    ),
    FORCE(
        "Force",
        listOf(
            EngUnit("N", "newton", 1.0), EngUnit("kN", "kilonewton", 1e3), EngUnit("MN", "meganewton", 1e6),
            EngUnit("kgf", "kilogram-force", G0), EngUnit("lbf", "pound-force", 4.4482216152605),
            EngUnit("kip", "kilopound-force", 4448.2216152605),
        ), "kN", "lbf",
    ),
    PRESSURE_GAUGE(
        "Pressure (gauge)",
        listOf(
            EngUnit("Pa(g)", "pascal gauge", 1.0), EngUnit("kPa(g)", "kilopascal gauge", 1e3),
            EngUnit("MPa(g)", "megapascal gauge", 1e6), EngUnit("bar(g)", "bar gauge", 1e5),
            EngUnit("psig", "psi gauge", 6894.757293168), EngUnit("kgf/cm²(g)", "kgf/cm² gauge", 98066.5),
        ), "bar(g)", "psig",
    ),
    PRESSURE_ABS(
        "Pressure (absolute)",
        listOf(
            EngUnit("Pa(a)", "pascal absolute", 1.0), EngUnit("kPa(a)", "kilopascal absolute", 1e3),
            EngUnit("MPa(a)", "megapascal absolute", 1e6), EngUnit("bar(a)", "bar absolute", 1e5),
            EngUnit("psia", "psi absolute", 6894.757293168), EngUnit("atm", "standard atmosphere", ATM_PA),
            EngUnit("mmHg", "millimetre of mercury", 133.322387415), EngUnit("inHg", "inch of mercury", 3386.389),
        ), "bar(a)", "psia",
    ),
    PRESSURE_DIFF(
        "Pressure difference",
        listOf(
            EngUnit("Pa", "pascal", 1.0), EngUnit("kPa", "kilopascal", 1e3), EngUnit("MPa", "megapascal", 1e6),
            EngUnit("bar", "bar", 1e5), EngUnit("mbar", "millibar", 100.0), EngUnit("psi", "psi", 6894.757293168),
            EngUnit("inH₂O", "inch of water", 249.08891), EngUnit("mmH₂O", "millimetre of water", 9.80665),
            EngUnit("ftH₂O", "foot of water", 2989.0669),
        ), "kPa", "psi",
    ),
    STRESS(
        "Stress",
        listOf(
            EngUnit("Pa", "pascal", 1.0), EngUnit("MPa", "megapascal", 1e6), EngUnit("N/mm²", "N per mm²", 1e6),
            EngUnit("GPa", "gigapascal", 1e9), EngUnit("kPa", "kilopascal", 1e3), EngUnit("psi", "psi", 6894.757293168),
            EngUnit("ksi", "kilopsi", 6894757.293168), EngUnit("Mpsi", "megapsi", 6894757293.168),
        ), "MPa", "psi",
    ),
    MODULUS(
        "Elastic modulus",
        listOf(
            EngUnit("Pa", "pascal", 1.0), EngUnit("GPa", "gigapascal", 1e9), EngUnit("MPa", "megapascal", 1e6),
            EngUnit("psi", "psi", 6894.757293168), EngUnit("ksi", "kilopsi", 6894757.293168),
            EngUnit("Mpsi", "megapsi", 6894757293.168),
        ), "GPa", "Mpsi",
    ),
    TEMPERATURE(
        "Temperature",
        listOf(
            EngUnit("K", "kelvin", 1.0), EngUnit("°C", "degree Celsius", 1.0, 273.15),
            EngUnit("°F", "degree Fahrenheit", 5.0 / 9.0, 273.15 - 32.0 * 5.0 / 9.0),
            EngUnit("°R", "degree Rankine", 5.0 / 9.0),
        ), "°C", "°F",
    ),
    TEMP_DIFF(
        "Temperature difference",
        listOf(EngUnit("K", "kelvin", 1.0), EngUnit("Δ°C", "Celsius degree", 1.0), EngUnit("Δ°F", "Fahrenheit degree", 5.0 / 9.0)),
        "Δ°C", "Δ°F",
    ),
    TIME(
        "Time",
        listOf(
            EngUnit("s", "second", 1.0), EngUnit("ms", "millisecond", 1e-3), EngUnit("min", "minute", 60.0),
            EngUnit("h", "hour", 3600.0), EngUnit("day", "day", 86400.0), EngUnit("yr", "year", 31_557_600.0),
        ), "s", "s",
    ),
    VELOCITY(
        "Velocity",
        listOf(
            EngUnit("m/s", "metre per second", 1.0), EngUnit("m/min", "metre per minute", 1.0 / 60),
            EngUnit("mm/min", "millimetre per minute", 1e-3 / 60), EngUnit("mm/s", "millimetre per second", 1e-3),
            EngUnit("km/h", "kilometre per hour", 1.0 / 3.6), EngUnit("ft/s", "foot per second", 0.3048),
            EngUnit("ft/min", "foot per minute", 0.00508), EngUnit("in/min", "inch per minute", 0.0254 / 60),
            EngUnit("mph", "mile per hour", 0.44704),
        ), "m/s", "ft/s",
    ),
    ACCELERATION(
        "Acceleration",
        listOf(EngUnit("m/s²", "metre per second²", 1.0), EngUnit("g", "standard gravity", G0), EngUnit("ft/s²", "foot per second²", 0.3048)),
        "m/s²", "ft/s²",
    ),
    VOLUME_FLOW(
        "Volume flow",
        listOf(
            EngUnit("m³/s", "cubic metre per second", 1.0), EngUnit("m³/h", "cubic metre per hour", 1.0 / 3600),
            EngUnit("L/s", "litre per second", 1e-3), EngUnit("L/min", "litre per minute", 1e-3 / 60),
            EngUnit("gpm", "US gallon per minute", 6.30901964e-5), EngUnit("cfm", "cubic foot per minute", 4.719474432e-4),
            EngUnit("bbl/d", "barrel per day", 0.158987294928 / 86400),
        ), "m³/h", "gpm",
    ),
    MASS_FLOW(
        "Mass flow",
        listOf(
            EngUnit("kg/s", "kilogram per second", 1.0), EngUnit("kg/h", "kilogram per hour", 1.0 / 3600),
            EngUnit("t/h", "tonne per hour", 1.0 / 3.6), EngUnit("lb/h", "pound per hour", 0.45359237 / 3600),
            EngUnit("lb/s", "pound per second", 0.45359237),
        ), "kg/h", "lb/h",
    ),
    DENSITY(
        "Density",
        listOf(
            EngUnit("kg/m³", "kilogram per cubic metre", 1.0), EngUnit("g/cm³", "gram per cm³", 1000.0),
            EngUnit("lb/ft³", "pound per cubic foot", 16.018463374), EngUnit("lb/in³", "pound per cubic inch", 27679.9047),
        ), "kg/m³", "lb/ft³",
    ),
    DYN_VISCOSITY(
        "Dynamic viscosity",
        listOf(EngUnit("Pa·s", "pascal second", 1.0), EngUnit("cP", "centipoise", 1e-3), EngUnit("lb/(ft·s)", "pound per foot-second", 1.488164)),
        "cP", "cP",
    ),
    KIN_VISCOSITY(
        "Kinematic viscosity",
        listOf(EngUnit("m²/s", "square metre per second", 1.0), EngUnit("cSt", "centistokes", 1e-6), EngUnit("ft²/s", "square foot per second", 0.09290304)),
        "cSt", "cSt",
    ),
    POWER(
        "Power",
        listOf(
            EngUnit("W", "watt", 1.0), EngUnit("kW", "kilowatt", 1e3), EngUnit("MW", "megawatt", 1e6),
            EngUnit("hp", "mechanical horsepower", 745.69987158), EngUnit("BTU/h", "BTU per hour", 0.29307107),
            EngUnit("TR", "ton of refrigeration", 3516.8528),
        ), "kW", "hp",
    ),
    ENERGY(
        "Energy",
        listOf(
            EngUnit("J", "joule", 1.0), EngUnit("kJ", "kilojoule", 1e3), EngUnit("MJ", "megajoule", 1e6),
            EngUnit("kWh", "kilowatt hour", 3.6e6), EngUnit("BTU", "British thermal unit", 1055.05585262),
            EngUnit("ft·lbf", "foot pound-force", 1.3558179483),
        ), "kJ", "BTU",
    ),
    TORQUE(
        "Torque / moment",
        listOf(
            EngUnit("N·m", "newton metre", 1.0), EngUnit("kN·m", "kilonewton metre", 1e3), EngUnit("N·mm", "newton millimetre", 1e-3),
            EngUnit("lbf·ft", "pound-force foot", 1.3558179483), EngUnit("lbf·in", "pound-force inch", 0.112984829),
            EngUnit("kip·ft", "kip foot", 1355.8179483),
        ), "N·m", "lbf·ft",
    ),
    AREA_MOMENT(
        "Second moment of area",
        listOf(EngUnit("m⁴", "metre⁴", 1.0), EngUnit("mm⁴", "millimetre⁴", 1e-12), EngUnit("cm⁴", "centimetre⁴", 1e-8), EngUnit("in⁴", "inch⁴", 4.162314256e-7)),
        "mm⁴", "in⁴",
    ),
    SECTION_MODULUS(
        "Section modulus",
        listOf(EngUnit("m³", "metre³", 1.0), EngUnit("mm³", "millimetre³", 1e-9), EngUnit("cm³", "centimetre³", 1e-6), EngUnit("in³", "inch³", 1.6387064e-5)),
        "mm³", "in³",
    ),
    ROT_SPEED(
        "Rotational speed",
        listOf(EngUnit("rad/s", "radian per second", 1.0), EngUnit("rpm", "revolution per minute", 2 * Math.PI / 60), EngUnit("rps", "revolution per second", 2 * Math.PI)),
        "rpm", "rpm",
    ),
    FREQUENCY(
        "Frequency",
        listOf(EngUnit("Hz", "hertz", 1.0), EngUnit("kHz", "kilohertz", 1e3), EngUnit("cpm", "cycle per minute", 1.0 / 60)),
        "Hz", "Hz",
    ),
    ANGLE(
        "Angle",
        listOf(EngUnit("rad", "radian", 1.0), EngUnit("°", "degree", Math.PI / 180)),
        "°", "°",
    ),
    LINEAR_DENSITY(
        "Mass per length",
        listOf(EngUnit("kg/m", "kilogram per metre", 1.0), EngUnit("lb/ft", "pound per foot", 1.48816394)),
        "kg/m", "lb/ft",
    ),
    FORCE_PER_LENGTH(
        "Force per length / stiffness",
        listOf(
            EngUnit("N/m", "newton per metre", 1.0), EngUnit("N/mm", "newton per millimetre", 1e3), EngUnit("kN/m", "kilonewton per metre", 1e3),
            EngUnit("lbf/in", "pound-force per inch", 175.126835), EngUnit("lbf/ft", "pound-force per foot", 14.5939029),
        ), "N/mm", "lbf/in",
    ),
    SPECIFIC_ENERGY(
        "Specific energy / enthalpy",
        listOf(EngUnit("J/kg", "joule per kilogram", 1.0), EngUnit("kJ/kg", "kilojoule per kilogram", 1e3), EngUnit("BTU/lb", "BTU per pound", 2326.0)),
        "kJ/kg", "BTU/lb",
    ),
    SPECIFIC_ENTROPY(
        "Specific entropy / heat capacity",
        listOf(EngUnit("J/(kg·K)", "joule per kilogram kelvin", 1.0), EngUnit("kJ/(kg·K)", "kilojoule per kilogram kelvin", 1e3), EngUnit("BTU/(lb·°F)", "BTU per pound °F", 4186.8)),
        "kJ/(kg·K)", "BTU/(lb·°F)",
    ),
    SPECIFIC_VOLUME(
        "Specific volume",
        listOf(EngUnit("m³/kg", "cubic metre per kilogram", 1.0), EngUnit("L/kg", "litre per kilogram", 1e-3), EngUnit("ft³/lb", "cubic foot per pound", 0.062427961)),
        "m³/kg", "ft³/lb",
    ),
    CONDUCTIVITY(
        "Thermal conductivity",
        listOf(EngUnit("W/(m·K)", "watt per metre kelvin", 1.0), EngUnit("BTU/(h·ft·°F)", "BTU per hour foot °F", 1.730734666)),
        "W/(m·K)", "BTU/(h·ft·°F)",
    ),
    HTC(
        "Heat transfer coefficient",
        listOf(EngUnit("W/(m²·K)", "watt per m² kelvin", 1.0), EngUnit("BTU/(h·ft²·°F)", "BTU per hour ft² °F", 5.678263341)),
        "W/(m²·K)", "BTU/(h·ft²·°F)",
    ),
    HEAT_FLUX(
        "Heat flux",
        listOf(EngUnit("W/m²", "watt per square metre", 1.0), EngUnit("BTU/(h·ft²)", "BTU per hour ft²", 3.154591)),
        "W/m²", "BTU/(h·ft²)",
    ),
    HEAT_PER_LENGTH(
        "Heat flow per length",
        listOf(EngUnit("W/m", "watt per metre", 1.0), EngUnit("BTU/(h·ft)", "BTU per hour foot", 0.9615173)),
        "W/m", "BTU/(h·ft)",
    ),
    EXPANSION(
        "Thermal expansion coefficient",
        listOf(
            EngUnit("1/K", "per kelvin", 1.0), EngUnit("µm/(m·K)", "micro-strain per kelvin", 1e-6),
            EngUnit("1/°F", "per °F", 1.8), EngUnit("µin/(in·°F)", "micro-strain per °F", 1.8e-6),
        ), "µm/(m·K)", "µin/(in·°F)",
    ),
    CURRENT("Electric current", listOf(EngUnit("A", "ampere", 1.0)), "A", "A"),
    VOLTAGE("Voltage", listOf(EngUnit("V", "volt", 1.0), EngUnit("kV", "kilovolt", 1e3)), "V", "V"),
    HEAT_INPUT(
        "Heat input per length",
        listOf(EngUnit("J/m", "joule per metre", 1.0), EngUnit("kJ/mm", "kilojoule per millimetre", 1e6), EngUnit("J/mm", "joule per millimetre", 1e3), EngUnit("kJ/in", "kilojoule per inch", 1e3 / 0.0254)),
        "kJ/mm", "kJ/in",
    ),
    MASS_INERTIA(
        "Mass moment of inertia",
        listOf(EngUnit("kg·m²", "kilogram metre²", 1.0), EngUnit("lb·ft²", "pound foot²", 0.04214011)),
        "kg·m²", "lb·ft²",
    ),
    HUMIDITY_RATIO(
        "Humidity ratio",
        listOf(EngUnit("kg/kg", "kg water per kg dry air", 1.0), EngUnit("g/kg", "g water per kg dry air", 1e-3), EngUnit("gr/lb", "grains per pound", 1.0 / 7000)),
        "g/kg", "gr/lb",
    ),
    ;

    fun unit(symbol: String): EngUnit = units.firstOrNull { it.symbol == symbol }
        ?: error("Unit '$symbol' is not defined for $name")

    fun defaultUnit(system: UnitSystem): EngUnit = unit(if (system == UnitSystem.SI) siDefault else usDefault)

    val si: EngUnit get() = units.first()
}

object Units {
    /** Converts [value] given in unit [from] to unit [to]; both must belong to [kind]. */
    fun convert(kind: Kind, value: Double, from: String, to: String): Double =
        kind.unit(to).fromSi(kind.unit(from).toSi(value))

    /** Gauge to absolute pressure (Pa). */
    fun abs(gaugePa: Double, atmPa: Double = ATM_PA) = gaugePa + atmPa

    /** Kinds offered in the standalone unit converter. */
    val converterKinds: List<Kind> = Kind.entries.filter { it.units.size > 1 }
}

/** Formats a number with [sig] significant figures, switching to scientific notation for extremes. */
fun formatNumber(value: Double, sig: Int = 4): String {
    if (value.isNaN()) return "—"
    if (value.isInfinite()) return if (value > 0) "∞" else "−∞"
    if (value == 0.0) return "0"
    val mag = kotlin.math.abs(value)
    if (mag >= 1e7 || mag < 1e-4) {
        return String.format(java.util.Locale.US, "%.${sig - 1}e", value)
    }
    val digits = kotlin.math.floor(kotlin.math.log10(mag)).toInt() + 1
    val decimals = (sig - digits).coerceIn(0, 10)
    val s = String.format(java.util.Locale.US, "%.${decimals}f", value)
    return if (s.contains('.')) s.trimEnd('0').trimEnd('.') else s
}
