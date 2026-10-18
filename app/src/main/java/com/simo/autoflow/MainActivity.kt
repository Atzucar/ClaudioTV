package com.simo.autoflow

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.view.View
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.concurrent.thread

object Session { var items: List<Item> = emptyList() }

/** Pantalla 1: qué quieres ver, con filtros Todo / Eventos / Películas. */
class MainActivity : Activity() {
    private lateinit var list: LinearLayout
    private lateinit var chipsRow: LinearLayout
    private lateinit var accChip: TextView
    private lateinit var vpnChip: TextView
    private lateinit var pTitle: TextView
    private lateinit var pCount: TextView
    private lateinit var updated: TextView
    private lateinit var warn: TextView
    private var filter = "all"
    private var firstLoad = true

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val c = this
        val root = LinearLayout(c).apply { 
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Ui.BG)
            setPadding(Ui.dp(c, 48), Ui.dp(c, 32), Ui.dp(c, 48), Ui.dp(c, 24)) 
        }

        // cabecera
        val header = LinearLayout(c).apply { gravity = Gravity.CENTER_VERTICAL }
        header.addView(Ui.tv(c, "▶", 14f, Ui.WHITE, true).apply { 
            gravity = Gravity.CENTER
            background = Ui.rounded(c, Ui.ACCENT, 8) 
        }, Ui.lp(Ui.dp(c, 28), Ui.dp(c, 28), r = Ui.dp(c, 10)))
        
        header.addView(Ui.tv(c, "Auto Flow", 15f, Ui.TEXT, true), Ui.lp(0, -2, 1f))
        vpnChip = Ui.pill(c, "", Ui.CHIP, Ui.SOFT)
        accChip = Ui.pill(c, "", Ui.CHIP, Ui.SOFT)
        updated = Ui.tv(c, "", 12f, Ui.MUTED)
        
        header.addView(updated, Ui.lp(-2, -2, r = Ui.dp(c, 12)))
        header.addView(filterChip(c, "↻ Recargar", false) { refresh() }, Ui.lp(-2, -2, r = Ui.dp(c, 8)))
        header.addView(vpnChip, Ui.lp(-2, -2, r = Ui.dp(c, 8)))
        header.addView(accChip)
        root.addView(header)

        root.addView(Ui.tv(c, "¿Qué quieres ver?", 36f, Ui.TEXT, true), Ui.lp(-2, -2, t = Ui.dp(c, 16), b = Ui.dp(c, 12)))

        warn = Ui.tv(c, "", 12f, Ui.RED).apply { visibility = View.GONE }
        root.addView(warn, Ui.lp(-2, -2, b = Ui.dp(c, 8)))
        chipsRow = LinearLayout(c)
        root.addView(chipsRow, Ui.lp(-2, -2, b = Ui.dp(c, 12)))

        // contenido: lista + panel
        val content = LinearLayout(c)
        list = LinearLayout(c).apply { orientation = LinearLayout.VERTICAL }
        content.addView(ScrollView(c).apply { isFillViewport = true; addView(list) }, Ui.lp(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f, r = Ui.dp(c, 28)))
        
        val panel = LinearLayout(c).apply { 
            orientation = LinearLayout.VERTICAL
            setPadding(Ui.dp(c, 20), Ui.dp(c, 20), Ui.dp(c, 20), Ui.dp(c, 20))
            background = Ui.rounded(c, Ui.CARD, 14) 
        }
        pTitle = Ui.tv(c, "", 20f, Ui.TEXT, true)
        pCount = Ui.tv(c, "", 13f, Ui.SOFT)
        panel.addView(Ui.tv(c, "SELECCIONADO", 11f, Ui.MUTED))
        panel.addView(pTitle, Ui.lp(-1, -2, t = Ui.dp(c, 10), b = Ui.dp(c, 10)))
        panel.addView(pCount)
        panel.addView(android.view.View(c), Ui.lp(1, 0, 1f))
        panel.addView(Ui.tv(c, "Pulsa OK para ver los enlaces.\nAl elegir uno se conecta la VPN y se abre AceStream.", 12f, Ui.SOFT))
        content.addView(panel, Ui.lp(Ui.dp(c, 260), ViewGroup.LayoutParams.MATCH_PARENT))
        root.addView(content, Ui.lp(-1, 0, 1f))

        root.addView(Ui.tv(c, "↑ ↓ Navegar      OK Seleccionar", 11f, Ui.MUTED), Ui.lp(-2, -2, t = Ui.dp(c, 10)))
        setContentView(root)

        buildChips()
        pTitle.text = "Cargando..."
        refresh()
    }

    private fun refresh() {
        updated.text = "Actualizando…"
        thread {
            val r = runCatching { Catalog.load(this) }.getOrNull()
            runOnUiThread {
                if (r == null) {
                    warn.text = "Error leyendo el catálogo (JSON no válido)."; warn.visibility = View.VISIBLE
                    updated.text = ""; pTitle.text = "Sin contenido"; return@runOnUiThread
                }
                Session.items = r.items
                warn.text = r.warning ?: ""; warn.visibility = if (r.warning == null) View.GONE else View.VISIBLE
                updated.text = "Actualizado ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())} · ${r.items.size} eventos · ${r.source}"
                buildList(focusFirst = firstLoad); firstLoad = false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val on = FlowService.instance != null
        accChip.text = if (on) "Accesibilidad · activa" else "Accesibilidad · INACTIVA"
        accChip.setTextColor(if (on) Ui.GREEN else Ui.RED)
        accChip.background = Ui.rounded(this, if (on) Ui.GREEN_BG else Ui.CHIP, 999)
        vpnChip.text = if (FlowService.vpnOk) "VPN · conectada" else "VPN · desconectada"
        vpnChip.setTextColor(if (FlowService.vpnOk) Ui.GREEN else Ui.SOFT)
    }

    private fun buildChips() {
        chipsRow.removeAllViews()
        listOf("all" to "Todo", "evento" to "Eventos", "pelicula" to "Películas").forEach { (key, label) ->
            chipsRow.addView(filterChip(this, label, filter == key) {
                filter = key; buildChips(); buildList(focusFirst = false)
                chipsRow.getChildAt(listOf("all", "evento", "pelicula").indexOf(key))?.requestFocus()
            }, Ui.lp(-2, -2, r = Ui.dp(this, 12)))
        }
    }

    private fun buildList(focusFirst: Boolean) {
        list.removeAllViews()
        
        // CORRECCIÓN: Filtra de manera elástica ignorando mayúsculas y minúsculas
        val shown = Session.items.withIndex().filter { 
            filter == "all" || it.value.type.lowercase().contains(filter.lowercase()) || it.value.type.isBlank()
        }
        
        if (shown.isEmpty()) { pTitle.text = "Sin contenido"; pCount.text = ""; return }
        shown.forEachIndexed { n, (idx, item) ->
            // Aseguramos que si item.type está vacío pinte "EVENTO" por defecto en la caja gris
            val displayType = if (item.type.isNullOrBlank()) "EVENTO" else item.type.uppercase()
            
            val row = FocusRow(this, displayType, item.title, null, item.time) {
                pTitle.text = item.title
                pCount.text = "${item.links.size} enlace" + if (item.links.size == 1) "" else "s disponibles"
            }
            row.setOnClickListener { startActivity(Intent(this, LinksActivity::class.java).putExtra("i", idx)) }
            list.addView(row, Ui.lp(-1, -2, b = Ui.dp(this, 8)))
            if (n == 0) { 
                pTitle.text = item.title
                pCount.text = "${item.links.size} enlaces"
                if (focusFirst) row.requestFocus() 
            }
        }
    }
}
