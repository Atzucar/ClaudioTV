package com.simo.autoflow

import android.content.Context
import org.json.JSONArray
import java.net.URL

data class Link(val name: String, val url: String, val quality: String?, val peers: Int?)
data class Item(val title: String, val type: String, val time: String?, val links: List<Link>)

data class CatalogResult(val items: List<Item>, val warning: String?)

object Catalog {
    /** Descarga Config.CATALOG_URL; si falla, usa assets/catalog.json y devuelve un aviso. Llamar fuera del hilo principal. */
    fun load(ctx: Context): CatalogResult {
        fun bundled() = parse(ctx.assets.open("catalog.json").bufferedReader().readText()).items
        if (Config.CATALOG_URL.isBlank()) return CatalogResult(bundled(), null)
        return try {
            val text = URL(Config.CATALOG_URL).openConnection().apply { connectTimeout = 8000; readTimeout = 8000 }
                .getInputStream().bufferedReader().readText()
            parse(text)
        } catch (e: Exception) {
            CatalogResult(bundled(), "No se pudo cargar el catálogo online (${e.message ?: e.javaClass.simpleName}). Mostrando el catálogo incluido.")
        }
    }

    /** JSON (empieza por '[') o texto simple: titulo | tipo | hora | enlace | calidad | url [| pares] por linea. */
    private fun parse(raw: String): CatalogResult {
        val text = raw.trim().removePrefix("\uFEFF")
        return if (text.startsWith("[")) CatalogResult(parseJson(text), null) else parseText(text)
    }

    private fun parseText(text: String): CatalogResult {
        val groups = LinkedHashMap<Triple<String, String, String?>, MutableList<Link>>()
        var skipped = 0
        for (line in text.lines().map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }) {
            val f = line.split("|").map { it.trim() }
            if (f.size < 6 || f[0].isEmpty() || !f[5].startsWith("acestream://", ignoreCase = true)) { skipped++; continue }
            val type = if (f[1].lowercase().startsWith("pel")) "pelicula" else "evento"
            val key = Triple(f[0], type, f[2].ifBlank { null })
            groups.getOrPut(key) { mutableListOf() }
                .add(Link(f[3].ifBlank { "Enlace" }, f[5], f[4].ifBlank { null }, f.getOrNull(6)?.toIntOrNull()))
        }
        val items = groups.map { (k, v) -> Item(k.first, k.second, k.third, v) }
        return CatalogResult(items, if (skipped > 0) "$skipped línea(s) ignorada(s): formato incorrecto." else null)
    }

    private fun parseJson(text: String): List<Item> {
        val arr = JSONArray(text)
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val ls = o.getJSONArray("links")
            Item(
                o.getString("title"),
                o.optString("type", "evento"),
                o.optString("time").ifBlank { null },
                (0 until ls.length()).map {
                    val l = ls.getJSONObject(it)
                    Link(l.getString("name"), l.getString("url"),
                        l.optString("quality").ifBlank { null },
                        if (l.has("peers")) l.getInt("peers") else null)
                })
        }
    }
}
