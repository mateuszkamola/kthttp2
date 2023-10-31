package pl.mkamola.kthttp2.net

import java.io.FileInputStream
import java.security.KeyStore
import java.util.concurrent.ArrayBlockingQueue
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLServerSocket
import javax.net.ssl.SSLSocket

class TcpServer(val keystoreFilepath: String, val keystorePassword: String) {
    var running = true
	val appProtocols = arrayOf("h2")
	val socketQueue = ArrayBlockingQueue<SSLSocket>(1024)

    fun start() {
        val ctx = initKeyStore()
        val socketFactory = ctx.getServerSocketFactory()
        val serverSocket = socketFactory.createServerSocket(9443) as? SSLServerSocket

        while (running) {
            val socket = serverSocket!!.accept() as? SSLSocket
            val sslParams = socket!!.sslParameters
            sslParams.applicationProtocols = appProtocols
            socket.sslParameters = sslParams
            socket.startHandshake()
           	socketQueue.add(socket) 
        }
    }

    fun initKeyStore(): SSLContext {
        val ctx = SSLContext.getInstance("TLS")

        val keystore = KeyStore.getInstance("PKCS12")
        keystore.load(FileInputStream(keystoreFilepath), keystorePassword.toCharArray())

        val kmf = KeyManagerFactory.getInstance("PKIX")
        kmf.init(keystore, keystorePassword.toCharArray())
        ctx.init(kmf.keyManagers, null, null)

        return ctx
    }
}
