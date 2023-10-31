package pl.mkamola.kthttp2

import pl.mkamola.kthttp2.net.TcpServer
import pl.mkamola.kthttp2.net.http2.Http2Server

fun main() {
    val tcpServer = TcpServer("domain.p12", "secret")
    println("Starting HTTP2 server")
	Http2Server(tcpServer).start()
}
