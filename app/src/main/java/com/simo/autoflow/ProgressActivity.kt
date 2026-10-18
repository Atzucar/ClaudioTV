package com.simo.autoflow

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView

/** Pantalla 3: progreso del proceso automatico. */
class ProgressActivity : Activity() {
    private lateinit var rows: List<TextView>
    private lateinit var info: TextView

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val c = this
        val root = LinearLayout(c).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setBackgroundColor(Ui.BG); setPadding(Ui.dp(c, 48), Ui.dp(c, 32), Ui.dp(c, 48), Ui.dp(c, 24)) }
        root.addView(Ui.tv(c, "PREPARANDO REPRODUCCIÓN", 11f, Ui.MUTED).apply { gravity = Gravity.CENTER })
        root.addView(Ui.tv(c, intent.getStringExtra("t") ?: "", 30f, Ui.TEXT, true).apply { gravity = Gravity.CENTER }, Ui.lp(-2, -2, t = Ui.dp(c, 8), b = Ui.dp(c, 32)))
        rows = FlowService.STEP_NAMES.map { Ui.tv(c, it, 17f, Ui.MUTED).apply { setPadding(Ui.dp(c, 20), Ui.dp(c, 16), Ui.dp(c, 20), Ui.dp(c, 16)) } }
        rows.forEach { root.addView(it, Ui.lp(Ui.dp(c, 460), -2, b = Ui.dp(c, 10))) }
        info = Ui.tv(c, "", 13f, Ui.SOFT).apply { gravity = Gravity.CENTER }
        root.addView(info, Ui.lp(-2, -2, t = Ui.dp(c, 16)))
        root.addView(Ui.tv(c, "Atrás  Cancelar", 12f, Ui.MUTED), Ui.lp(-2, -2, t = Ui.dp(c, 24)))
        setContentView(root)
    }

    override fun onResume() { super.onResume(); FlowService.onChange = { runOnUiThread { render() } }; render() }
    override fun onPause() { super.onPause(); FlowService.onChange = null }

    override fun onBackPressed() { FlowService.instance?.cancel(); super.onBackPressed() }

    private fun render() {
        val st = FlowService.steps
        rows.forEachIndexed { i, v ->
            val name = FlowService.STEP_NAMES[i]
            when (st[i]) {
                FlowService.DONE -> { v.text = "✓  $name"; v.setTextColor(Ui.GREEN); v.background = Ui.rounded(this, Ui.GREEN_BG, 12) }
                FlowService.ACTIVE -> { v.text = "◌  $name…"; v.setTextColor(Ui.TEXT); v.background = Ui.rounded(this, Ui.ACCENT, 12, Ui.WHITE, 2) }
                FlowService.ERROR -> { v.text = "✕  $name"; v.setTextColor(Ui.RED); v.background = Ui.rounded(this, Ui.CARD, 12) }
                else -> { v.text = "○  $name"; v.setTextColor(Ui.MUTED); v.background = Ui.rounded(this, Ui.CARD, 12) }
            }
        }
        info.text = FlowService.lastError ?: if (st.all { it == FlowService.DONE }) "Listo" else ""
        info.setTextColor(if (FlowService.lastError != null) Ui.RED else Ui.SOFT)
    }
}
