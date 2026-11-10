package com.shilin.cuoci

import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket

/**
 * 局域网直传（双向同步）：
 * 一台等待（服务端），另一台连过去；一次连接后双方互换数据，各自合并。
 */
object LanTransfer {

    const val PORT = 8899

    /** 本机所有 IPv4 地址（排除 127.x） */
    fun localIps(): List<String> {
        val out = ArrayList<String>()
        try {
            for (nif in NetworkInterface.getNetworkInterfaces()) {
                if (!nif.isUp || nif.isLoopback) continue
                for (addr in nif.inetAddresses) {
                    val a = addr as? Inet4Address ?: continue
                    val ip = a.hostAddress ?: continue
                    if (ip.startsWith("127.")) continue
                    if (!out.contains(ip)) out.add(ip)
                }
            }
        } catch (e: Exception) {
            LogStore.add("LAN", "取 IP 失败：" + e.message)
        }
        return out
    }

    fun bestIp(): String {
        val ips = localIps()
        return ips.firstOrNull { it.startsWith("192.168.") }
            ?: ips.firstOrNull { it.startsWith("10.") }
            ?: ips.firstOrNull { it.startsWith("172.") }
            ?: ips.firstOrNull() ?: ""
    }

    /**
     * 服务端：等对方连上来 → 先读对方数据 → 回调让我方合并 → 再把（合并后的）自己发过去。
     *
     * @param provideJson 每次调用都返回当前完整数据的 JSON（合并后要能取到最新）
     * @param onRemote 收到对方数据（后台线程调用），调用方在这里合并
     */
    fun serve(
        provideJson: () -> String,
        timeoutMs: Int,
        onState: (String) -> Unit,
        onRemote: (String) -> Boolean
    ): Boolean {
        var server: ServerSocket? = null
        var sock: Socket? = null
        try {
            server = ServerSocket(PORT)
            server.reuseAddress = true
            server.soTimeout = timeoutMs
            onState("等待另一台连接…")
            sock = server.accept()
            sock.soTimeout = timeoutMs

            onState("已连接，接收对方数据…")
            val remote = readData(sock.getInputStream())
            onState("收到，正在合并…")
            val ok = onRemote(String(remote, Charsets.UTF_8))

            onState("回传本机数据…")
            writeData(sock.getOutputStream(), provideJson().toByteArray(Charsets.UTF_8))
            try {
                Thread.sleep(200)
            } catch (e: InterruptedException) {
            }
            onState(if (ok) "双向同步完成 ✅" else "数据有问题，请重试")
            return ok
        } catch (e: Exception) {
            onState("失败：" + (e.message ?: "超时或已取消"))
            return false
        } finally {
            try {
                sock?.close()
            } catch (e: Exception) {
            }
            try {
                server?.close()
            } catch (e: Exception) {
            }
        }
    }

    /**
     * 客户端：连上 → 先把自己发过去 → 再读对方（合并后的）数据。
     */
    fun sync(
        ip: String,
        myJson: String,
        timeoutMs: Int,
        onState: (String) -> Unit
    ): String? {
        var sock: Socket? = null
        try {
            onState("正在连接 " + ip + "…")
            sock = Socket()
            sock.connect(InetSocketAddress(ip, PORT), 8000)
            sock.soTimeout = timeoutMs

            onState("已连接，发送本机数据…")
            writeData(sock.getOutputStream(), myJson.toByteArray(Charsets.UTF_8))

            onState("等待对方数据…")
            val remote = readData(sock.getInputStream())
            onState("收到，正在合并…")
            return String(remote, Charsets.UTF_8)
        } catch (e: Exception) {
            onState("失败：" + (e.message ?: "连不上，检查是否同一网络"))
            return null
        } finally {
            try {
                sock?.close()
            } catch (e: Exception) {
            }
        }
    }

    private fun writeData(out: java.io.OutputStream, data: ByteArray) {
        val dos = DataOutputStream(out)
        dos.writeInt(data.size)
        dos.write(data)
        dos.flush()
    }

    private fun readData(inp: java.io.InputStream): ByteArray {
        val din = DataInputStream(inp)
        val len = din.readInt()
        if (len <= 0 || len > 20 * 1024 * 1024) throw RuntimeException("数据长度异常")
        val buf = ByteArray(len)
        din.readFully(buf)
        return buf
    }
}
