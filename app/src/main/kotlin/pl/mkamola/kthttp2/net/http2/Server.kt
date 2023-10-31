package pl.mkamola.kthttp2.net.http2

import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import javax.net.ssl.SSLSocket
import pl.mkamola.kthttp2.net.TcpServer

typealias FrameType = Byte 

class FrameTypes {
	companion object {
		const val DATA : Byte = 0
		const val HEADERS : Byte = 1
		const val SETTINGS : Byte = 4
	}
}

class Http2Server(val tcpServer: TcpServer) {
    val socketQueue = tcpServer.socketQueue
    var running = true

    fun start() {
        val tcpServerThread = Thread({ tcpServer.start() })
        tcpServerThread.start()
        while (running) {
            val socket = socketQueue.take()
            Thread(SocketHandler(socket)).start()
        }
    }

    class SocketHandler(val socket: SSLSocket) : Runnable {
        val outgoingBuffer = ByteArray(65536)
        val incomingBuffer = ByteArray(65536)
        val buf = ByteBuffer.allocate(1 shl 21)
        val preface =
                byteArrayOf(
                        0x50,
                        0x52,
                        0x49,
                        0x20,
                        0x2a,
                        0x20,
                        0x48,
                        0x54,
                        0x54,
                        0x50,
                        0x2f,
                        0x32,
                        0x2e,
                        0x30,
                        0x0d,
                        0x0a,
                        0x0d,
                        0x0a,
                        0x53,
                        0x4d,
                        0x0d,
                        0x0a,
                        0x0d,
                        0x0a
                )

        override fun run() {
            buf.limit(0)
            val inputStream = socket.getInputStream()
            val outputStream = socket.getOutputStream()
            send(outputStream, preface)
            send(outputStream, serialize(createSettingsFrame()))
            receivePreface(inputStream)
            while (true) {
                val f = readFrame(inputStream)
                handleFrame(f)
            }
        }

        fun handleFrame(frame: Frame) {
            when (frame.type) {
				FrameTypes.HEADERS -> handleHeadersFrame(frame)
				FrameTypes.DATA -> handleDataFrame(frame)
				FrameTypes.SETTINGS -> handleSettingsFrame(frame)
			}

        }

		fun handleSettingsFrame(frame: Frame) {
		
		}

		fun handleHeadersFrame(frame: Frame) {
			println("Got headers frame: $frame")
			val buf = ByteBuffer.wrap(frame.payload)
			var paddingBytes : Byte = 0
			if ((frame.flags.toInt() and 0x8) == 0x8) {
				//padding enabled
				paddingBytes = buf.get()
			}
			var streamDep = 0 
			var weight : Byte = 0
			if ((frame.flags.toInt() and 0x20) == 0x20) {
				streamDep = buf.getInt() and 0x7fffffff
				weight = buf.get()
			}
			println("streamDep $streamDep, weight $weight, padding $paddingBytes")
		}

		fun handleDataFrame(frame: Frame) {
		}

        fun receivePreface(inputStream: InputStream) {
            val maybePreface = readBytes(inputStream, 24)
            if (!maybePreface.contentEquals(preface)) {
                throw RuntimeException("Incorrect preface received, abort!")
            }
        }

        fun readBytes(inputStream: InputStream, count: Int): ByteArray {
            ensureBufferSize(inputStream, count)
            val bytes = ByteArray(count)
            buf.get(bytes)
            return bytes
        }

        fun ensureBufferSize(inputStream: InputStream, count: Int) {
            if (buf.remaining() < count) {
                buf.compact()
                val n = inputStream.read(incomingBuffer)
                buf.put(incomingBuffer, 0, n)
                buf.flip()
            }
        }

        fun readFrame(inputStream: InputStream): Frame {
            ensureBufferSize(inputStream, 9)
            val length =
                    (buf.get().toInt() and 0xff shl 16) or
                            (buf.get().toInt() and 0xff shl 8) or
                            (buf.get().toInt() and 0xff)
            val type = buf.get()
            val flags = buf.get()
            val streamId = buf.getInt()
            ensureBufferSize(inputStream, length)
            val payload = ByteArray(length)
            buf.get(payload)
            return Frame(length.toInt(), type, flags, streamId, payload)
        }

        fun send(output: OutputStream, data: ByteArray) {
            output.write(data)
        }

        fun createSettingsFrame(): Frame {
            val length = 0
            val type: Byte = 4
            val flags: Byte = 0
            val streamId = 2
            return Frame(length, type, flags, streamId, ByteArray(0))
        }

        fun serialize(frame: Frame): ByteArray {
            val buffer = ByteBuffer.allocate(frame.length + 9)
            buffer.put((frame.length shr 16).toByte())
            buffer.put((frame.length shr 8).toByte())
            buffer.put(frame.length.toByte())
            buffer.put(frame.type)
            buffer.put(frame.flags)
            buffer.putInt(frame.streamIdentifier)
            buffer.put(frame.payload)
            buffer.flip()
            return buffer.array()
        }
    }

    data class Frame(
            val length: Int,
            val type: Byte,
            val flags: Byte,
            val streamIdentifier: Int,
            val payload: ByteArray,
    )
}
