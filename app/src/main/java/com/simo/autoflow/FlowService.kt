package com.simo.autoflow

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.*

class FlowService : AccessibilityService() {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var job: Job? = null

    override fun onServiceConnected() { instance = this }
    override fun onUnbind(i: Intent?): Boolean { instance = null; scope.cancel(); return super.onUnbind(i) }
    override fun onAccessibilityEvent(e: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    fun cancel() { job?.cancel() }

    fun runFlow(streamUrl: String) {
        job?.cancel()
        for (i in steps.indices) steps[i] = PENDING
        lastError = null; notifyChange()
        job = scope.launch {
            var cur = 0
            try {
                cur = 0; set(0, ACTIVE)
                launchApp(Config.PROTON_PKG)
                delay(4000)
                val already = waitFor(Config.CONNECTED_TEXTS, 3000) != null && findText(Config.COUNTRY_TEXTS) != null
                set(0, DONE)
                cur = 1; set(1, ACTIVE)
                if (!already) connectVpn()
                vpnOk = true
                set(1, DONE)
                cur = 2; set(2, ACTIVE)
                playStream(streamUrl)
                set(2, DONE)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Flujo fallido: ${e.message}")
                lastError = e.message; set(cur, ERROR)
            }
        }
    }

    private fun set(i: Int, st: Int) { steps[i] = st; notifyChange() }
    private fun notifyChange() { onChange?.invoke() }

    private suspend fun connectVpn() {
        // Buscar Rusia en la lista (desplazando) y pulsarla.
        val country = scrollFind(Config.COUNTRY_TEXTS) ?: error("No encuentro Rusia en ProtonVPN")
        click(country)
        delay(1500)
        // Algunas versiones conectan al tocar el pais; si no, pulsar Connect.
        waitFor(Config.CONNECT_TEXTS, 5000)?.let { click(it) }
        waitFor(Config.CONNECTED_TEXTS, Config.VPN_TIMEOUT_MS) ?: error("VPN no conecto")
        delay(2000)
    }

    private suspend fun playStream(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        // Si Fire TV pregunta con que app abrirlo, elegir AceStream.
        waitFor(listOf("Ace Stream", "AceStream", "Ace Player"), 6000)?.let { click(it) }
    }

    // ---- utilidades ----
    private fun launchApp(pkg: String) {
        val i = packageManager.getLeanbackLaunchIntentForPackage(pkg) ?: packageManager.getLaunchIntentForPackage(pkg)
            ?: error("App no instalada: $pkg")
        startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP))
    }

    private fun findText(texts: List<String>): AccessibilityNodeInfo? {
        val root = rootInActiveWindow ?: return null
        for (t in texts) root.findAccessibilityNodeInfosByText(t).firstOrNull()?.let { return it }
        return null
    }

    private suspend fun waitFor(texts: List<String>, timeout: Long = Config.STEP_TIMEOUT_MS): AccessibilityNodeInfo? {
        val end = System.currentTimeMillis() + timeout
        while (System.currentTimeMillis() < end) { findText(texts)?.let { return it }; delay(500) }
        return null
    }

    private suspend fun scrollFind(texts: List<String>): AccessibilityNodeInfo? {
        repeat(25) {
            findText(texts)?.let { return it }
            val sc = findScrollable(rootInActiveWindow)
            if (sc != null) sc.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD) else return null
            delay(700)
        }
        return null
    }

    private fun findScrollable(n: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (n == null) return null
        if (n.isScrollable) return n
        for (i in 0 until n.childCount) findScrollable(n.getChild(i))?.let { return it }
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

    companion object {
        const val TAG = "AutoFlow"
        const val PENDING = 0; const val ACTIVE = 1; const val DONE = 2; const val ERROR = 3
        val STEP_NAMES = listOf("Abriendo ProtonVPN", "Conectando a Rusia", "Abriendo AceStream")
        val steps = IntArray(3)
        @Volatile var lastError: String? = null
        @Volatile var vpnOk = false
        var instance: FlowService? = null
        var onChange: (() -> Unit)? = null
    }
}
