package me.lucat1.sock.reader

import kotlinx.coroutines.*

import java.io.IOException
import java.io.File
import java.net.StandardProtocolFamily
import java.net.UnixDomainSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.ServerSocketChannel
import kotlin.system.exitProcess

class Server(sock: String, output: String) {
    private val sockFile: File = File(sock)
    private val sock: UnixDomainSocketAddress = UnixDomainSocketAddress.of(sockFile.path)

    private val outputFile: File = File(output)

    fun run() {
        try {
            sockFile.delete()
        } catch (e: Exception) {
            println("Could not bind delete previous UNIX socket at ${sockFile.path}")
            e.printStackTrace()
            exitProcess(5)
        }
        val channel = ServerSocketChannel.open(StandardProtocolFamily.UNIX)
        try {
            channel.bind(sock)
        } catch (e: IOException) {
            println("Could not bind to UNIX socket on ${sockFile.path}")
            e.printStackTrace()
            exitProcess(6)
        }

        println("Listening on ${sockFile.path}, writing to ${outputFile.path}")

        runBlocking {
            while (true) {
                val client = channel.accept()
                launch {
                    println("Received client connection")
                    val buffer = ByteBuffer.allocate(256)
                    client.read(buffer)
                    buffer.flip()

                    // Print the received message
                    val message = String(buffer.array(), 0, buffer.limit())
                    println("Received from client: $message")

                    // Write a response back to the client
                    val response = "Hello from server!"
                    buffer.clear()
                    buffer.put(response.toByteArray())
                    buffer.flip()
                    client.write(buffer)
                    println("Response sent to client")
                }
            }
        }
    }
}