package com.simo.autoflow

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.net.Uri
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.*
import java.util.concurrent.CancellationException

class FlowService : AccessibilityService() {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var job: Job? = null

    // Estados del flujo
    private val PENDING = 0
    private val ACTIVE = 1
    private val DONE = 2
    private val ERROR = 3

    private val steps = IntArray(3) { PENDING }
    private var lastError: String? = null
    private var vpnOk = false

    override fun onAccessibilityEvent(event: android.view.accessibility.AccessibilityEvent?) {
        // El servicio reacciona a los cambios de pantalla, pero la lógica corre en las corrutinas de runFlow
    }

    override fun onInterrupt() {}

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val streamUrl = intent? .getStringExtra("EXTRA_STREAM_URL")
        if (streamUrl != null) {
            runFlow(streamUrl)
        }
        return START_STICKY
    }

    // 1. El flujo completo automatizado
    fun runFlow(streamUrl: String) {
        job? .cancel()
        for (i in steps.indices) steps[i] = PENDING
        lastError = null
        notifyChange()
        
        job = scope.launch {
            var cur = 0
            try {
                // Paso 0: Abrir ProtonVPN
                cur = 0
                set(0, ACTIVE)
                launchApp(Config.PROTON_PKG)
                delay(4000)
                
                // Verificar si ya está conectado previamente
                val already = waitFor(Config.CONNECTED_TEXTS, 3000) != null && findText(Config.COUNTRY_TEXTS) != null
                set(0, DONE)
                
                // Paso 1: Conectar a Rusia
                cur = 1
                set(1, ACTIVE)
                if (!already) {
                    connectVpn()
                }
                vpnOk = true
                set(1, DONE)
                
                // Paso 2: Abrir el enlace en AceStream
                cur = 2
                set(2, ACTIVE)
                playStream(streamUrl)
                set(2, DONE)
                
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lastError = e.message
                set(cur, ERROR)
            }
        }
    }

    // 2. Conexión a Rusia en ProtonVPN
    private suspend fun connectVpn() {
        val country = scrollFind(Config.COUNTRY_TEXTS) ?: error("No encuentro Rusia en ProtonVPN")
        click(country)
        delay(1500)
        
        // Algunas versiones conectan al tocar el país; si no, pulsar botón Connect
        waitFor(Config.CONNECT_TEXTS, 5000)?.let { click(it) }
        
        // Esperar la confirmación de la conexión segura
        waitFor(Config.CONNECTED_TEXTS, Config.VPN_TIMEOUT_MS) ?: error("VPN no conectó")
        delay(2000)
    }

    // 3. Lanzar la reproducción en AceStream
    private suspend fun playStream(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
        
        // Si Fire TV pregunta con qué aplicación abrir el protocolo acestream://, elegir AceStream
        waitFor(listOf("Ace Stream", "AceStream", "Ace Player"), 6000)?.let { click(it) }
    }

    // 4. Utilidades de búsqueda en pantalla y simulación de clics físicos
    private fun findText(texts: List<String>): AccessibilityNodeInfo? {
        val root = rootInActiveWindow ?: return null
        for (t in texts) {
            root.findAccessibilityNodeInfosByText(t).firstOrNull()?.let { return it }
        }
        return null
    }

    private suspend fun waitFor(texts: List<String>, timeout: Long = Config.STEP_TIMEOUT_MS): AccessibilityNodeInfo? {
        val end = System.currentTimeMillis() + timeout
        while (System.currentTimeMillis() < end) {
            findText(texts)?.let { return it }
            delay(500)
        }
        return null
    }

    private suspend fun scrollFind(texts: List<String>): AccessibilityNodeInfo? {
        repeat(25) {
            findText(texts)?.let { return it }
            val sc = findScrollable(rootInActiveWindow)
            if (sc != null) {
                sc.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
            } else {
                // Alternativa para Fire OS si falla el contenedor scrollable: enviar D-PAD Down virtual
                performGlobalAction(GLOBAL_ACTION_RECENTS) // Reemplazable por pulsación D-PAD en hardware real
                return null
            }
            delay(700)
        }
        return null
    }

    private fun findScrollable(n: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (n == null) return null
        if (n.isScrollable) return n
        for (i in 0 until n.childCount) {
            findScrollable(n.getChild(i))?.let { return it }
        }
        return null
    }

    private fun click(n: AccessibilityNodeInfo) {
        var c: AccessibilityNodeInfo? = n
        while (c != null) {
            if (c.isClickable && c.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return
            c = c.parent
        }
        n.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
    }

    private fun launchApp(packageName: String) {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(launchIntent)
        } else {
            error("La aplicación objetivo no está instalada: $packageName")
        }
    }

    private fun set(stepIndex: Int, status: Int) {
        if (stepIndex in steps.indices) {
            steps[stepIndex] = status
            notifyChange()
        }
    }

    private fun notifyChange() {
        // Enlaza con tu Ui.kt pasándole el estado actual de la barra naranja (image_y-8cgH.png)
    }

    override fun onDestroy() {
        super.onDestroy()
        job? .cancel()
    }
}
