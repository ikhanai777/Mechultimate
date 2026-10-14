package com.mechultimate.core.engine

import com.mechultimate.core.units.Kind

enum class Module(val title: String, val subtitle: String) {
    PIPING("Piping", "ASME B31.1 / B31.3 / B31.4 / B31.8, flow, supports"),
    RELIEF("Pressure Relief", "API 520 / 521 / 526"),
    VESSELS("Pressure Vessels", "ASME BPVC Section VIII Div. 1"),
    TANKS("Storage Tanks", "API 650 / 653"),
    INSPECTION("Inspection & Integrity", "API 510 / 570, ISO 20816, ISO 21940"),
    MACHINE("Machine Design", "Shafts, bearings, bolts, springs, fits, welds"),
    STRUCTURES("Strength & Structures", "Beams, columns, stress, torsion"),
    FLUIDS("Fluids & Pumps", "Pumps, NPSH, valves, flow"),
    THERMO("Thermo & Heat Transfer", "Steam tables, exchangers, conduction"),
    HVAC("HVAC", "Psychrometrics, air flow, ducts"),
    WELDING("Welding & Materials", "ASME IX, AWS D1.1, carbon equivalent"),
    MANUFACTURING("Manufacturing", "Machining, sheet metal, threads"),
    GENERAL("General", "Vibration, geometry, utilities"),
}

data class Reference(val code: String, val clause: String = "", val note: String = "") {
    override fun toString() = listOf(code, clause, note).filter { it.isNotBlank() }.joinToString(" · ")
}

data class Option(val key: String, val label: String)

/** Shows an input only while choice input [choiceId] has one of the [values]. */
data class VisibleIf(val choiceId: String, val values: Set<String>)

sealed interface InputSpec {
    val id: String
    val label: String
    val help: String
    val visibleIf: VisibleIf?
}

/** A numeric input. [default], [min] and [max] are in the SI unit of [kind]. */
data class NumberInput(
    override val id: String,
    override val label: String,
    val kind: Kind,
    val default: Double,
    val min: Double? = null,
    val max: Double? = null,
    val minExclusive: Boolean = false,
    override val help: String = "",
    val siUnit: String? = null,
    val usUnit: String? = null,
    override val visibleIf: VisibleIf? = null,
) : InputSpec

data class ChoiceInput(
    override val id: String,
    override val label: String,
    val options: List<Option>,
    val default: String,
    override val help: String = "",
    override val visibleIf: VisibleIf? = null,
) : InputSpec

/** Input values keyed by input id. Numbers are SI. */
data class InputValues(val numbers: Map<String, Double>, val choices: Map<String, String>)

enum class Status { PASS, WARN, FAIL, INFO }

data class Step(
    val label: String,
    val formula: String,
    val value: Double?,
    val kind: Kind?,
    val reference: String?,
    val siUnit: String? = null,
    val usUnit: String? = null,
    val text: String? = null,
)

data class Output(
    val label: String,
    val kind: Kind?,
    val value: Double?,
    val text: String? = null,
    val primary: Boolean = false,
    val siUnit: String? = null,
    val usUnit: String? = null,
)

data class Check(val status: Status, val message: String)

data class CalcResult(
    val outputs: List<Output> = emptyList(),
    val steps: List<Step> = emptyList(),
    val checks: List<Check> = emptyList(),
    val error: String? = null,
    val fieldErrors: Map<String, String> = emptyMap(),
) {
    val overall: Status
        get() = when {
            error != null || fieldErrors.isNotEmpty() || checks.any { it.status == Status.FAIL } -> Status.FAIL
            checks.any { it.status == Status.WARN } -> Status.WARN
            checks.any { it.status == Status.PASS } -> Status.PASS
            else -> Status.INFO
        }
}

class CalcException(message: String) : Exception(message)

class Calculator internal constructor(
    val id: String,
    val title: String,
    val module: Module,
    val description: String,
    val keywords: List<String>,
    val references: List<Reference>,
    val inputs: List<InputSpec>,
    val assumptions: List<String>,
    private val body: CalcScope.() -> Unit,
) {
    fun defaults() = InputValues(
        numbers = inputs.filterIsInstance<NumberInput>().associate { it.id to it.default },
        choices = inputs.filterIsInstance<ChoiceInput>().associate { it.id to it.default },
    )

    fun isVisible(spec: InputSpec, values: InputValues): Boolean {
        val cond = spec.visibleIf ?: return true
        return values.choices[cond.choiceId] in cond.values
    }

    fun validate(values: InputValues): Map<String, String> {
        val errors = mutableMapOf<String, String>()
        for (spec in inputs) {
            if (spec !is NumberInput || !isVisible(spec, values)) continue
            val v = values.numbers[spec.id]
            when {
                v == null || v.isNaN() -> errors[spec.id] = "Required"
                spec.min != null && (v < spec.min || (spec.minExclusive && v == spec.min)) ->
                    errors[spec.id] = if (spec.minExclusive) "Must be greater than minimum" else "Below minimum"
                spec.max != null && v > spec.max -> errors[spec.id] = "Above maximum"
            }
        }
        return errors
    }

    fun compute(values: InputValues): CalcResult {
        val merged = InputValues(defaults().numbers + values.numbers, defaults().choices + values.choices)
        val fieldErrors = validate(merged)
        if (fieldErrors.isNotEmpty()) return CalcResult(fieldErrors = fieldErrors, error = "Check highlighted inputs")
        val scope = CalcScope(merged)
        return try {
            scope.body()
            val bad = scope.outputs.any { o -> o.value != null && !o.value.isFinite() }
            CalcResult(
                scope.outputs, scope.steps, scope.checks,
                error = if (bad) "Result is not finite — check inputs are physically consistent" else null,
            )
        } catch (e: CalcException) {
            CalcResult(scope.outputs, scope.steps, scope.checks, error = e.message)
        } catch (e: ArithmeticException) {
            CalcResult(scope.outputs, scope.steps, scope.checks, error = "Arithmetic error: ${e.message}")
        }
    }
}

/** Receiver for a calculator body: reads inputs and records the trace, results and checks. */
class CalcScope(private val values: InputValues) {
    internal val steps = mutableListOf<Step>()
    internal val outputs = mutableListOf<Output>()
    internal val checks = mutableListOf<Check>()

    fun n(id: String): Double = values.numbers[id] ?: throw CalcException("Missing input $id")
    fun c(id: String): String = values.choices[id] ?: throw CalcException("Missing choice $id")

    /** Records an intermediate step and returns [value]. Set [output] to also list it as a result. */
    fun step(
        label: String,
        formula: String,
        kind: Kind?,
        value: Double,
        ref: String? = null,
        output: Boolean = false,
        primary: Boolean = false,
        si: String? = null,
        us: String? = null,
    ): Double {
        steps += Step(label, formula, value, kind, ref, si, us)
        if (output || primary) outputs += Output(label, kind, value, primary = primary, siUnit = si, usUnit = us)
        return value
    }

    fun out(label: String, kind: Kind?, value: Double, primary: Boolean = false, si: String? = null, us: String? = null): Double {
        outputs += Output(label, kind, value, primary = primary, siUnit = si, usUnit = us)
        return value
    }

    fun text(label: String, text: String, primary: Boolean = false, formula: String? = null, ref: String? = null) {
        outputs += Output(label, null, null, text = text, primary = primary)
        if (formula != null) steps += Step(label, formula, null, null, ref, text = text)
    }

    fun check(ok: Boolean, okMessage: String, badMessage: String, severity: Status = Status.FAIL) {
        checks += if (ok) Check(Status.PASS, okMessage) else Check(severity, badMessage)
    }

    fun warn(message: String) { checks += Check(Status.WARN, message) }
    fun info(message: String) { checks += Check(Status.INFO, message) }
    fun fail(message: String): Nothing = throw CalcException(message)
    fun require(ok: Boolean, message: String) { if (!ok) fail(message) }
}

class CalculatorBuilder(private val id: String, private val title: String, private val module: Module) {
    var description = ""
    private val keywords = mutableListOf<String>()
    private val refs = mutableListOf<Reference>()
    private val inputs = mutableListOf<InputSpec>()
    private val assumptions = mutableListOf<String>()
    private var body: (CalcScope.() -> Unit)? = null

    fun keywords(vararg k: String) { keywords += k }
    fun ref(code: String, clause: String = "", note: String = "") { refs += Reference(code, clause, note) }
    fun assume(vararg a: String) { assumptions += a }

    fun number(
        id: String, label: String, kind: Kind, default: Double,
        min: Double? = null, max: Double? = null, minExclusive: Boolean = false,
        help: String = "", si: String? = null, us: String? = null, visibleIf: VisibleIf? = null,
    ) { inputs += NumberInput(id, label, kind, default, min, max, minExclusive, help, si, us, visibleIf) }

    /** Convenience for a strictly positive input. */
    fun positive(
        id: String, label: String, kind: Kind, default: Double,
        help: String = "", si: String? = null, us: String? = null, max: Double? = null, visibleIf: VisibleIf? = null,
    ) = number(id, label, kind, default, 0.0, max, true, help, si, us, visibleIf)

    fun choice(id: String, label: String, options: List<Pair<String, String>>, default: String = options.first().first, help: String = "", visibleIf: VisibleIf? = null) {
        inputs += ChoiceInput(id, label, options.map { Option(it.first, it.second) }, default, help, visibleIf)
    }

    fun compute(block: CalcScope.() -> Unit) { body = block }

    internal fun build(): Calculator {
        val ids = inputs.map { it.id }
        check(ids.size == ids.toSet().size) { "$id: duplicate input ids" }
        return Calculator(id, title, module, description, keywords, refs, inputs, assumptions, body ?: error("$id: no compute block"))
    }
}

fun calculator(id: String, title: String, module: Module, block: CalculatorBuilder.() -> Unit): Calculator =
    CalculatorBuilder(id, title, module).apply(block).build()

fun visibleIf(choiceId: String, vararg values: String) = VisibleIf(choiceId, values.toSet())
