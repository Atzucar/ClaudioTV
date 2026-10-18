package com.simo.autoflow

/** Edita estos valores antes de compilar. */
object Config {
    // Paquetes (Fire TV). Si alguno no coincide, cambialo (adb shell pm list packages | grep ...)
    const val PROTON_PKG = "ch.protonvpn.android"

    // Textos de la UI de ProtonVPN (en cualquiera de los idiomas del dispositivo)
    val COUNTRY_TEXTS = listOf("Russia", "Rusia")
    val CONNECT_TEXTS = listOf("Connect", "Conectar", "Quick Connect")
    val CONNECTED_TEXTS = listOf("Disconnect", "Desconectar", "Connected", "Conectado")

    // Catalogo: URL de un JSON (ver assets/catalog.json). Vacio = usa el catalogo incluido en la app.
    const val CATALOG_URL = "https://gist.githubusercontent.com/Atzucar/cab11f83f863212d6e815001ed1377ef/raw/catalog.txt"

    const val STEP_TIMEOUT_MS = 20_000L
    const val VPN_TIMEOUT_MS = 60_000L
}
