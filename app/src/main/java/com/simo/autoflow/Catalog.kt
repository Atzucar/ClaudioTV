package com.simo.autoflow

import android.content.Context
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

data class Link(val name: String, val url: String, val quality: String?, val peers: Int?)
data class Item(val title: String, val type: String, val time: String?, val links: List<Link>)
data class CatalogResult(val items: List<Item>, val warning: String?, val source: String)

object Catalog {
    fun load(ctx: Context): CatalogResult {
        fun bundled() = try {
            parseJson(ctx.assets.open("catalog.json").bufferedReader().readText())
        } catch (e: Exception) {
            emptyList()
        }

        if (Config.CATALOG_URL.isBlank()) {
            return CatalogResult(bundled(), "No hay CATALOG_URL configurada.", "ejemplo")
        }

        return try {
            val conn = URL(Config.CATALOG_URL + "?t=" + System.currentTimeMillis()).openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.useCaches = false
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android TV) Gecko/120.0")
            
            if (conn.responseCode != 200) error("HTTP ${conn.responseCode}")
            
            val rawText = conn.inputStream.bufferedReader().use { it.readText() }
            val itemsList = parseJson(rawText)
            
            if (itemsList.isEmpty()) {
                CatalogResult(bundled(), "El archivo online no contiene eventos válidos.", "online")
            } else {
                CatalogResult(itemsList, null, "online")
            }
        } catch (e: Exception) {
            CatalogResult(bundled(), "Error de red: ${e.message}", "ejemplo")
        }
    }

    private fun parseJson(text: String): List<Item> {
        val arr = JSONArray(text)
        val items = mutableListOf<Item>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            
            // Busca de forma elástica tanto "links" como "channels"
            val key = when {
                o.has("links") -> "links"
                o.has("channels") -> "channels"
                else -> null
            }
            if (key == null) continue
            
            val ls = o.getJSONArray(key)
            val linkList = (0 until ls.length()).map {
                val l = ls.getJSONObject(it)
                Link(
                    l.optString("name", "Canal ${it + 1}"),
                    l.getString("url"),
                    l.optString("quality", null),
                    if (l.has("peers")) l.optInt("peers") else null
                )
            }
            items.add(
                Item(
                    o.getString("title"),
                    if (o.optString("type", "evento").lowercase().startsWith("pel")) "pelicula" else "evento",
                    o.optString("time", null),
                    linkList
                )
            )
        }
        return items
    }
}


