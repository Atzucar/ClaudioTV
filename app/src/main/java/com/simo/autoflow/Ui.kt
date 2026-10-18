package com.simo.autoflow

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

object Ui {
    val BG = 0xFF0E1116.toInt()
    val CARD = 0xFF171C24.toInt()
    val CARD2 = 0xFF252C37.toInt()
    val CHIP = 0xFF1A2029.toInt()
    val ACCENT = 0xFFE8590C.toInt()
    val TEXT = 0xFFF2F4F8.toInt()
    val TEXT2 = 0xFFD5DBE5.toInt()
    val SOFT = 0xFFA9B3C2.toInt()
    val MUTED = 0xFF7C8797.toInt()
    val GREEN = 0xFF6FE08D.toInt()
    val GREEN_BG = 0xFF17301F.toInt()
    val RED = 0xFFFF6B6B.toInt()
    val WHITE = 0xFFFFFFFF.toInt()

    fun dp(c: Context, v: Int) = (v * c.resources.displayMetrics.density).toInt()

    fun rounded(c: Context, color: Int, radius: Int, stroke: Int = 0, strokeDp: Int = 0) =
        GradientDrawable().apply {
            setColor(color); cornerRadius = dp(c, radius).toFloat()
            if (strokeDp > 0) setStroke(dp(c, strokeDp), stroke)
        }

    fun tv(c: Context, text: String, sp: Float, color: Int, bold: Boolean = false) = TextView(c).apply {
        this.text = text; textSize = sp; setTextColor(color)
        typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
    }

    fun pill(c: Context, text: String, bg: Int, fg: Int) = tv(c, text, 11f, fg).apply {
        setPadding(dp(c, 12), dp(c, 6), dp(c, 12), dp(c, 6)); background = rounded(c, bg, 999)
    }

    fun lp(w: Int, h: Int, weight: Float = 0f, l: Int = 0, t: Int = 0, r: Int = 0, b: Int = 0) =
        LinearLayout.LayoutParams(w, h, weight).apply { setMargins(l, t, r, b) }
}

/** Fila enfocable con el mando: [lead] titulo [badge] [trail]. */
class FocusRow(
    c: Context, lead: String?, title: String, badge: String?, trail: String?,
    private val onFocused: () -> Unit = {},
) : LinearLayout(c) {
    private val leadV = lead?.let { Ui.tv(c, it, 10f, Ui.SOFT, true) }
    private val titleV = Ui.tv(c, title, 19f, Ui.TEXT2)
    private val badgeV = badge?.let { Ui.tv(c, it, 11f, Ui.SOFT, true) }
    private val trailV = trail?.let { Ui.tv(c, it, 13f, Ui.MUTED) }

    init {
        orientation = HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
        isFocusable = true; isClickable = true
        setPadding(Ui.dp(c, 18), Ui.dp(c, 14), Ui.dp(c, 18), Ui.dp(c, 14))
        leadV?.let { it.setPadding(Ui.dp(c, 8), Ui.dp(c, 4), Ui.dp(c, 8), Ui.dp(c, 4)); addView(it, Ui.lp(-2, -2, r = Ui.dp(c, 16))) }
        addView(titleV, Ui.lp(0, -2, 1f))
        badgeV?.let { it.setPadding(Ui.dp(c, 8), Ui.dp(c, 4), Ui.dp(c, 8), Ui.dp(c, 4)); addView(it, Ui.lp(-2, -2, l = Ui.dp(c, 12))) }
        trailV?.let { addView(it, Ui.lp(-2, -2, l = Ui.dp(c, 16))) }
        onFocusChangeListener = OnFocusChangeListener { _, f -> style(f); if (f) onFocused() }
        style(false)
    }

    private fun style(f: Boolean) {
        val c = context
        background = if (f) Ui.rounded(c, Ui.ACCENT, 12, Ui.WHITE, 2) else Ui.rounded(c, Ui.CARD, 12)
        titleV.setTextColor(if (f) Ui.TEXT else Ui.TEXT2)
        titleV.typeface = if (f) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT
        val chipBg = if (f) 0x33FFFFFF else Ui.CARD2
        leadV?.apply { setTextColor(if (f) Ui.TEXT else Ui.SOFT); background = Ui.rounded(c, chipBg, 6) }
        badgeV?.apply { setTextColor(if (f) Ui.TEXT else Ui.SOFT); background = Ui.rounded(c, chipBg, 6) }
        trailV?.setTextColor(if (f) Ui.TEXT else Ui.MUTED)
    }
}

/** Boton tipo "pildora" para filtros. */
fun filterChip(c: Context, label: String, selected: Boolean, onClick: () -> Unit): TextView =
    Ui.tv(c, label, 13f, if (selected) Ui.BG else Ui.SOFT, true).apply {
        isFocusable = true; isClickable = true
        setPadding(Ui.dp(c, 16), Ui.dp(c, 8), Ui.dp(c, 16), Ui.dp(c, 8))
        background = Ui.rounded(c, if (selected) Ui.WHITE else Ui.CHIP, 999)
        setOnClickListener { onClick() }
        onFocusChangeListener = View.OnFocusChangeListener { v, f ->
            v.background = Ui.rounded(c, if (selected) Ui.WHITE else Ui.CHIP, 999, if (f) Ui.ACCENT else 0, if (f) 3 else 0)
        }
    }
