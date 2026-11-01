package com.safespot.scanner.vpn

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import de.blinkt.openvpn.core.ConfigParser
import de.blinkt.openvpn.core.ConnectionStatus
import de.blinkt.openvpn.core.IOpenVPNServiceInternal
import de.blinkt.openvpn.core.OpenVPNService
import de.blinkt.openvpn.core.ProfileManager
import de.blinkt.openvpn.core.VPNLaunchHelper
import de.blinkt.openvpn.core.VpnStatus
import java.io.StringReader
import kotlinx.coroutines.launch
import kotlinx.coroutines.async

/** Thin wrapper around ics-openvpn (GPL-2) exposing Compose state for the UI. */
object Vpn : VpnStatus.StateListener, VpnStatus.ByteCountListener {
    enum class State { OFF, CONNECTING, CONNECTED, ERROR }
    var state by mutableStateOf(State.OFF); private set
    var detail by mutableStateOf(""); private set
    var server by mutableStateOf<VpnServer?>(null); private set
    var bytesIn by mutableLongStateOf(0L); private set
    var bytesOut by mutableLongStateOf(0L); private set
    var connectedAt by mutableLongStateOf(0L); private set
    var sessions by mutableIntStateOf(0); private set
    /** Last 60 samples of down/up speed in bytes/s (real, from OpenVPN's byte counter). */
    val speedDown = mutableStateListOf<Float>(); val speedUp = mutableStateListOf<Float>()

    private var bound = false
    private var service: IOpenVPNServiceInternal? = null
    private val conn = object : ServiceConnection {
        override fun onServiceConnected(n: ComponentName?, b: IBinder?) { service = IOpenVPNServiceInternal.Stub.asInterface(b) }
        override fun onServiceDisconnected(n: ComponentName?) { service = null }
    }

    fun init(ctx: Context) {
        if (bound) return
        VpnStatus.addStateListener(this); VpnStatus.addByteCountListener(this)
        bound = ctx.applicationContext.bindService(Intent(ctx, OpenVPNService::class.java).setAction(OpenVPNService.START_SERVICE), conn, Context.BIND_AUTO_CREATE)
    }

    /** VPN Gate relays are mostly old SoftEther servers: allow their CBC cipher and legacy certs. */
    fun patchConfig(cfg: String, safeDns: Boolean): String {
        // Drop the long/infinite retry settings shipped in VPN Gate configs; we fail over ourselves.
        val drop = Regex("^(resolv-retry|connect-retry|connect-retry-max|connect-timeout|server-poll-timeout|ping-restart|hand-window)\\b.*")
        val sb = StringBuilder(cfg.replace("\r", "").lines().filterNot { drop.matches(it.trim()) }.joinToString("\n"))
        sb.append("\nconnect-timeout 6\nserver-poll-timeout 6\nhand-window 10\n")
        if (!cfg.contains("data-ciphers")) sb.append("\ndata-ciphers AES-256-GCM:AES-128-GCM:AES-128-CBC\ndata-ciphers-fallback AES-128-CBC\n")
        return sb.toString()
    }

    /** Caller must have obtained VpnService.prepare() consent first. */
    fun connect(ctx: Context, s: VpnServer, safeDns: Boolean): String? = try {
        val cp = ConfigParser(); cp.parseConfig(StringReader(patchConfig(s.config, safeDns)))
        val vp = cp.convertProfile()
        vp.mName = "SafeSpot · ${s.country}"
        vp.mTlSCertProfile = "legacy"
        vp.mConnectRetry = "1"; vp.mConnectRetryMax = "1"; vp.mConnectRetryMaxTime = "2"
        vp.mConnections?.forEach { it.mConnectTimeout = 6 }
        if (safeDns) { vp.mOverrideDNS = true; vp.mDNS1 = "9.9.9.9"; vp.mDNS2 = "149.112.112.112" }
        ProfileManager.setTemporaryProfile(ctx, vp)
        server = s; state = State.CONNECTING; detail = "Starting…"
        bytesIn = 0; bytesOut = 0; speedDown.clear(); speedUp.clear()
        VPNLaunchHelper.startOpenVpn(vp, ctx.applicationContext, "user", true)
        null
    } catch (e: Exception) { state = State.ERROR; detail = e.message ?: "Bad config"; detail }

    // ---- Smart connect: probe, pick first responsive, auto-failover ----
    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Main)
    private var job: kotlinx.coroutines.Job? = null
    @Volatile private var failingOver = false
    var attempt by mutableIntStateOf(0); private set
    const val ATTEMPT_TIMEOUT_MS = 12_000L

    /**
     * [cands] ranked servers. If [probe], the top 8 are TCP-probed in parallel (1.5 s) and the first to answer is used;
     * otherwise cands[0] is tried first. Each attempt gets ~12 s; then the next responsive server is tried (max 4).
     */
    fun connectSmart(ctx: Context, cands: List<VpnServer>, safeDns: Boolean, probe: Boolean, onPicked: (VpnServer) -> Unit = {}) {
        job?.cancel(); if (cands.isEmpty()) { state = State.ERROR; detail = "No servers available"; return }
        state = State.CONNECTING; detail = if (probe) "Finding the fastest server…" else "Starting…"; attempt = 0
        job = scope.launch {
            // Manual pick connects at once; failover candidates are probed in parallel meanwhile.
            val probedD = async { Servers.fastest(if (probe) cands else cands.drop(1), 8) }
            val queue = ArrayList<VpnServer>()
            if (probe) { queue += probedD.await(); if (queue.isEmpty()) queue += cands.take(3) } else queue += cands[0]
            var i = 0
            while (i < queue.size && i < 4) { val sv = queue[i]
                attempt = i + 1
                if (i > 0) { failingOver = true; runCatching { service?.stopVPN(false) }; kotlinx.coroutines.delay(400); failingOver = false; detail = "Trying faster server… (${sv.country})" }
                onPicked(sv)
                connect(ctx, sv, safeDns)?.let { return@launch }
                if (i > 0) detail = "Trying faster server… (${sv.country})"
                val ok = kotlinx.coroutines.withTimeoutOrNull(ATTEMPT_TIMEOUT_MS) { while (state != State.CONNECTED) { if (state == State.ERROR && !failingOver) break; kotlinx.coroutines.delay(150) }; state == State.CONNECTED }
                failingOver = false
                if (ok == true) return@launch
                if (state == State.OFF) return@launch // user cancelled
                i++
                if (i == 1 && !probe) { queue += probedD.await().filter { p -> queue.none { it.id == p.id } }; if (queue.size == 1) queue += cands.drop(1).take(2) }
            }
            runCatching { service?.stopVPN(false) }; state = State.ERROR; detail = "No server answered. Try again or pick another country."
        }
    }

    fun disconnect(ctx: Context) {
        job?.cancel(); failingOver = false
        ProfileManager.setConnectedVpnProfileDisconnected(ctx)
        runCatching { service?.stopVPN(false) }
        state = State.OFF; detail = "Disconnected"; connectedAt = 0
    }

    private val main = android.os.Handler(android.os.Looper.getMainLooper())
    override fun updateState(st: String?, log: String?, resId: Int, level: ConnectionStatus?, intent: Intent?) { main.post { onState(st, log, level) } }
    private fun onState(st: String?, log: String?, level: ConnectionStatus?) {
        val newState = when (level) {
            ConnectionStatus.LEVEL_CONNECTED -> State.CONNECTED
            ConnectionStatus.LEVEL_NOTCONNECTED -> if (state == State.CONNECTING && (st == "NOPROCESS" || failingOver || job?.isActive == true)) State.CONNECTING else State.OFF
            ConnectionStatus.LEVEL_AUTH_FAILED -> if (job?.isActive == true) State.CONNECTING else State.ERROR
            ConnectionStatus.LEVEL_START, ConnectionStatus.LEVEL_CONNECTING_SERVER_REPLIED, ConnectionStatus.LEVEL_CONNECTING_NO_SERVER_REPLY_YET,
            ConnectionStatus.LEVEL_WAITING_FOR_USER_INPUT -> State.CONNECTING
            else -> state
        }
        if (newState == State.CONNECTED && state != State.CONNECTED) { connectedAt = SystemClock.elapsedRealtime(); sessions++ }
        if (newState != State.CONNECTED) connectedAt = 0
        state = newState
        if (!failingOver) detail = listOfNotNull(st, log?.takeIf { it.isNotBlank() }).joinToString(": ")
    }

    override fun setConnectedVPN(uuid: String?) {}

    override fun updateByteCount(inB: Long, outB: Long, diffIn: Long, diffOut: Long) { main.post { onBytes(inB, outB, diffIn, diffOut) } }
    private fun onBytes(inB: Long, outB: Long, diffIn: Long, diffOut: Long) {
        bytesIn = inB; bytesOut = outB
        // OpenVPN reports every ~2 s (management interval); convert to bytes/s
        speedDown += diffIn / 2f; speedUp += diffOut / 2f
        while (speedDown.size > 60) speedDown.removeAt(0)
        while (speedUp.size > 60) speedUp.removeAt(0)
    }
}
