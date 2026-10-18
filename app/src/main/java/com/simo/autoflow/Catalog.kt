package com.simo.autoflow

import android.content.Context
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

data class Link(val name: String, val url: String, val quality: String?, val peers: Int?)
data class Item(val title: String, val type: String, val time: String?, val links: List<Link>)

data class CatalogResult(val items: List<Item>, val warning: String?, val source: String)

object Catalog {
    private const val HINT = "Formato: titulo | tipo | hora | canal | calidad | acestream://HASH"

    /** Descarga Config.CATALOG_URL; si falla, usa assets/catalog.json y avisa. Llamar fuera del hilo principal. */
    fun load(ctx: Context): CatalogResult {
        fun bundled() = parse(ctx.assets.open("catalog.json").bufferedReader().readText(), "ejemplo").items
        if (Config.CATALOG_URL.isBlank()) return CatalogResult(bundled(), "No hay CATALOG_URL en Config.kt: se muestra el catálogo de ejemplo.", "ejemplo")
        return try {
            val conn = URL(Config.CATALOG_URL + "?t=" + System.currentTimeMillis()).openConnection() as HttpURLConnection
            conn.connectTimeout = 8000; conn.readTimeout = 8000; conn.useCaches = false
            if (conn.responseCode != 200) error("HTTP ${conn.responseCode}")
            val r = parse(conn.inputStream.bufferedReader().readText(), "online")
            if (r.items.isEmpty()) r.copy(warning = "El archivo online no contiene ningún evento válido. ${r.warning ?: HINT}") else r
        } catch (e: Exception) {
            CatalogResult(bundled(), "No se pudo cargar el catálogo online (${e.message ?: e.javaClass.simpleName}). Se muestra el de ejemplo.", "ejemplo")
        }
    }

    /** JSON (empieza por '[') o texto simple, una linea por enlace. */
    private fun parse(raw: String, source: String): CatalogResult {
        val text = raw.trim().removePrefix("\uFEFF")
        return if (text.startsWith("[")) CatalogResult(parseJson(text), null, source) else parseText(text, source)
    }

    private fun parseText(text: String, source: String): CatalogResult {
        val groups = LinkedHashMap<Triple<String, String, String?>, MutableList<Link>>()
        val skipped = mutableListOf<Int>()
        text.lines().forEachIndexed { n, rawLine ->
            val line = rawLine.trim().trim('|').trim()
            if (line.isEmpty() || line.startsWith("#")) return@forEachIndexed
            val f = line.split("|").map { it.trim() }
            // 6 campos (con calidad) o 5 (sin calidad): titulo | tipo | hora | canal | [calidad] | url [| pares]
            val hasQuality = f.size >= 6
            val url = normalizeUrl(if (hasQuality) f[5] else f.getOrNull(4))
            if (f.size < 5 || f[0].isEmpty() || url == null) { skipped.add(n + 1); return@forEachIndexed }
            val type = if (f[1].lowercase().startsWith("pel")) "pelicula" else "evento"
            val key = Triple(f[0], type, f[2].ifBlank { null })
            groups.getOrPut(key) { mutableListOf() }.add(Link(
                f[3].ifBlank { "Enlace" }, url,
                if (hasQuality) f[4].ifBlank { null } else null,
                f.getOrNull(6)?.toIntOrNull()))
        }
        val items = groups.map { (k, v) -> Item(k.first, k.second, k.third, v) }
        val warn = if (skipped.isEmpty()) null else
            "${skipped.size} línea(s) ignorada(s) (nº ${skipped.take(5).joinToString(", ")}${if (skipped.size > 5) "…" else ""}). $HINT"
        return CatalogResult(items, warn, source)
    }

    /** Acepta acestream://HASH o un hash suelto de 40 caracteres. */
    private fun normalizeUrl(u: String?): String? = when {
        u == null -> null
        u.startsWith("acestream://", ignoreCase = true) && u.length > 12 -> u
        Regex("^[0-9a-fA-F]{40}$").matches(u) -> "acestream://$u"
        else -> null
    }

    private fun parseJson(text: String): List<Item> {
        val arr = JSONArray(text)
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val ls = o.getJSONArray("links")
            Item(
                o.getString("title"),
                if (o.optString("type", "evento").lowercase().startsWith("pel")) "pelicula" else "evento",
                o.optString("time").ifBlank { null },
                (0 until ls.length()).map {
                    val l = ls.getJSONObject(it)
                    Link(l.getString("name"), l.getString("url"),
                        l.optString("quality").ifBlank { null },
                        if (l.has("peers")) l.optInt("peers") else null)
                })
        }
    }
}
