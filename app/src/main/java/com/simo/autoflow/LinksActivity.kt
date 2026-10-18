package com.simo.autoflow

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView

/** Pantalla 2: enlaces de AceStream. Al elegir uno se lanza todo el proceso. */
class LinksActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val c = this
        val item = Session.items[intent.getIntExtra("i", 0)]
        val root = LinearLayout(c).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Ui.BG); setPadding(Ui.dp(c, 48), Ui.dp(c, 32), Ui.dp(c, 48), Ui.dp(c, 24)) }
        root.addView(Ui.tv(c, "‹  Atrás · Contenidos", 13f, Ui.SOFT))
        root.addView(Ui.tv(c, item.type.uppercase() + (item.time?.let { " · $it" } ?: ""), 11f, Ui.MUTED), Ui.lp(-2, -2, t = Ui.dp(c, 24)))
        root.addView(Ui.tv(c, item.title, 36f, Ui.TEXT, true), Ui.lp(-2, -2, t = Ui.dp(c, 6)))
        root.addView(Ui.tv(c, "Elige un enlace de AceStream", 14f, Ui.SOFT), Ui.lp(-2, -2, t = Ui.dp(c, 6), b = Ui.dp(c, 20)))

        val list = LinearLayout(c).apply { orientation = LinearLayout.VERTICAL }
        item.links.forEachIndexed { n, link ->
            val row = FocusRow(c, "▶", link.name, link.quality, link.peers?.let { "$it pares" })
            row.setOnClickListener {
                val s = FlowService.instance
                if (s == null) android.widget.Toast.makeText(c, "Activa primero el servicio de accesibilidad", android.widget.Toast.LENGTH_LONG).show()
                else {
                    s.runFlow(link.url)
                    startActivity(Intent(c, ProgressActivity::class.java).putExtra("t", "${item.title} · ${link.name}"))
                }
            }
            list.addView(row, Ui.lp(-1, -2, b = Ui.dp(c, 8)))
            if (n == 0) row.requestFocus()
        }
        root.addView(ScrollView(c).apply { addView(list) }, Ui.lp(-1, 0, 1f))
        root.addView(Ui.tv(c, "↑ ↓ Navegar      OK Reproducir      Atrás Volver", 11f, Ui.MUTED), Ui.lp(-2, -2, t = Ui.dp(c, 10)))
        setContentView(root)
    }
}
