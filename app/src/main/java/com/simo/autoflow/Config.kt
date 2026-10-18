package com.simo.autoflow

object Config {
    const val PROTON_PKG = "ch.protonvpn.android"

    val COUNTRY_TEXTS = listOf("Russia", "Rusia")
    val CONNECT_TEXTS = listOf("Connect", "Conectar", "Quick Connect")
    val CONNECTED_TEXTS = listOf("Disconnect", "Desconectar", "Connected", "Conectado")

    // URL del archivo generado automáticamente por tu scraper
    const val CATALOG_URL = "https://githubusercontent.com"

    const val STEP_TIMEOUT_MS = 20_000L
    const val VPN_TIMEOUT_MS = 60_000L
}
