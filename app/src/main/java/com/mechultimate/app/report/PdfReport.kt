package com.mechultimate.app.report

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.mechultimate.app.BuildConfig
import com.mechultimate.app.data.Settings
import com.mechultimate.app.ui.CalculatorViewModel
import com.mechultimate.app.ui.DISCLAIMER
import com.mechultimate.app.ui.ResultFormatter
import com.mechultimate.core.engine.Calculator
import com.mechultimate.core.engine.ChoiceInput
import com.mechultimate.core.engine.NumberInput
import com.mechultimate.core.engine.Status
import java.io.File
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Renders a calculation sheet (A4) with header, inputs, results, checks, step-by-step work,
 * references and a content hash so the printed sheet can be matched to its inputs.
 */
object PdfReport {
    private const val W = 595 // A4 at 72 dpi
    private const val H = 842
    private const val M = 40f

    fun write(context: Context, calc: Calculator, title: String, project: String, vm: CalculatorViewModel, fmt: ResultFormatter, settings: Settings): File {
        val result = vm.result
        val values = vm.values()
        val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
        val hash = MessageDigest.getInstance("SHA-256")
            .digest((calc.id + values.numbers.toSortedMap() + values.choices.toSortedMap() + BuildConfig.VERSION_NAME).toByteArray())
            .joinToString("") { "%02x".format(it) }.take(16)

        val doc = PdfDocument()
        val w = Writer(doc, footer = "${calc.id} · $hash · MechUltimate ${BuildConfig.VERSION_NAME} · page ")

        w.text(settings.company.ifBlank { "Calculation sheet" }, 9f, bold = true, color = Color.DKGRAY)
        w.text(title, 16f, bold = true)
        w.text("${calc.id} — ${calc.title}", 10f)
        w.text("Project: $project    Prepared by: ${settings.engineer.ifBlank { "—" }}    Date: $stamp", 9f, color = Color.DKGRAY)
        w.rule()

        w.heading("Inputs")
        for (spec in calc.inputs) {
            if (!calc.isVisible(spec, values)) continue
            when (spec) {
                is NumberInput -> {
                    val f = vm.fields.getValue(spec.id)
                    w.row(spec.label, "${f.text} ${f.unit}")
                }
                is ChoiceInput -> w.row(spec.label, spec.options.first { it.key == values.choices[spec.id] }.label)
            }
        }

        w.heading("Results — ${result.overall.label()}")
        result.error?.let { w.text("Error: $it", 10f, color = Color.rgb(185, 28, 28)) }
        for (o in result.outputs) w.row(o.label, fmt.output(o), bold = o.primary)
        if (result.checks.isNotEmpty()) {
            w.heading("Code checks")
            for (c in result.checks) w.text("[${c.status.label()}] ${c.message}", 9.5f)
        }

        w.heading("Calculation steps")
        result.steps.forEachIndexed { i, s ->
            w.text("${i + 1}. ${s.label}", 9.5f, bold = true)
            w.text(s.formula, 9f, mono = true, indent = 14f)
            val v = fmt.step(s)
            if (v.isNotBlank()) w.text("= $v" + (s.reference?.let { "      [$it]" } ?: ""), 9f, mono = true, indent = 14f)
        }

        w.heading("References")
        calc.references.forEach { w.text("• $it", 9f) }
        if (calc.assumptions.isNotEmpty()) {
            w.heading("Assumptions")
            calc.assumptions.forEach { w.text("• $it", 9f) }
        }
        w.rule()
        w.text(DISCLAIMER, 7.5f, color = Color.DKGRAY)
        w.text("Content hash $hash identifies this calculator, these inputs and the app version.", 7.5f, color = Color.DKGRAY)
        w.finish()

        val dir = File(context.cacheDir, "reports").apply { mkdirs() }
        val file = File(dir, "${calc.id}_${title.replace(Regex("[^A-Za-z0-9_-]+"), "_").take(40)}.pdf")
        file.outputStream().use { doc.writeTo(it) }
        doc.close()
        return file
    }

    private fun Status.label() = when (this) { Status.PASS -> "PASS"; Status.WARN -> "CHECK"; Status.FAIL -> "FAIL"; Status.INFO -> "INFO" }

    /** Simple flowing text layout with automatic page breaks. */
    private class Writer(private val doc: PdfDocument, private val footer: String) {
        private var pageNo = 0
        private lateinit var page: PdfDocument.Page
        private lateinit var canvas: Canvas
        private var y = 0f

        init { newPage() }

        private fun newPage() {
            if (pageNo > 0) finishPage()
            pageNo++
            page = doc.startPage(PdfDocument.PageInfo.Builder(W, H, pageNo).create())
            canvas = page.canvas
            y = M
        }

        private fun finishPage() {
            val p = paint(7.5f, color = Color.GRAY)
            canvas.drawText(footer + pageNo, M, H - 20f, p)
            doc.finishPage(page)
        }

        fun finish() = finishPage()

        private fun paint(size: Float, bold: Boolean = false, mono: Boolean = false, color: Int = Color.BLACK) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            this.color = color
            typeface = Typeface.create(if (mono) Typeface.MONOSPACE else Typeface.SANS_SERIF, if (bold) Typeface.BOLD else Typeface.NORMAL)
        }

        private fun ensure(h: Float) { if (y + h > H - 40f) newPage() }

        private fun wrap(text: String, p: Paint, width: Float): List<String> {
            val lines = mutableListOf<String>()
            for (para in text.split('\n')) {
                var line = ""
                for (word in para.split(' ')) {
                    val trial = if (line.isEmpty()) word else "$line $word"
                    if (p.measureText(trial) <= width) line = trial else { if (line.isNotEmpty()) lines += line; line = word }
                }
                lines += line
            }
            return lines
        }

        fun text(s: String, size: Float, bold: Boolean = false, mono: Boolean = false, color: Int = Color.BLACK, indent: Float = 0f) {
            val p = paint(size, bold, mono, color)
            for (line in wrap(s, p, W - 2 * M - indent)) {
                ensure(size * 1.4f)
                y += size * 1.3f
                canvas.drawText(line, M + indent, y, p)
            }
        }

        fun heading(s: String) { y += 8f; text(s, 11.5f, bold = true, color = Color.rgb(11, 92, 173)); y += 2f }

        fun row(label: String, value: String, bold: Boolean = false) {
            val lp = paint(9.5f, color = Color.DKGRAY)
            val vp = paint(9.5f, bold = bold, mono = true)
            val labelLines = wrap(label, lp, 250f)
            val valueLines = wrap(value, vp, W - 2 * M - 260f)
            val n = maxOf(labelLines.size, valueLines.size)
            ensure(n * 13f)
            for (i in 0 until n) {
                y += 12.5f
                labelLines.getOrNull(i)?.let { canvas.drawText(it, M, y, lp) }
                valueLines.getOrNull(i)?.let { canvas.drawText(it, M + 260f, y, vp) }
            }
        }

        fun rule() {
            ensure(10f)
            y += 6f
            canvas.drawLine(M, y, W - M, y, paint(1f, color = Color.LTGRAY).apply { strokeWidth = 0.8f })
            y += 4f
        }
    }
}
