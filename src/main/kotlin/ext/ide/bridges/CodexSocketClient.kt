package ext.ide.bridges

import java.net.StandardProtocolFamily
import java.net.UnixDomainSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel
import java.nio.file.Path
import kotlin.io.path.exists

object CodexSocketClient {
    private const val SOCKET_PATH = "/tmp/ide_bridge.sock"

    fun sendMessage(msg: String): String {
        val socketPath = Path.of(SOCKET_PATH)
        if (!socketPath.exists()) {
            return "Error: Socket file does not exist ($SOCKET_PATH) \nIs C server running?"
        }

        return try {
            val address = UnixDomainSocketAddress.of(socketPath)

            val client = SocketChannel.open(StandardProtocolFamily.UNIX)
            client.connect(address)

            val buffer = ByteBuffer.wrap(msg.toByteArray())
            client.write(buffer)

            val responseBuffer = ByteBuffer.allocate(1024)
            val bytesRead = client.read(responseBuffer)

            val response = if (bytesRead > 0) {
                String(responseBuffer.array(), 0, bytesRead).trim()
            }else {
                "Received empty response from C server"
            }

            client.close()
            response
        } catch (e: Exception) {
            "Socket Error: ${e.message}`"
        }
    }
}