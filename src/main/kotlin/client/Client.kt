package me.lucat1.sock.reader

import java.io.File

import java.net.StandardProtocolFamily
import java.net.UnixDomainSocketAddress
import java.nio.channels.ServerSocketChannel

class Client(sock: String) {
    private val sockFile: File = File(sock)
    val sock: UnixDomainSocketAddress = UnixDomainSocketAddress.of(sockFile.path)

    fun run() {
        val channel = ServerSocketChannel.open(StandardProtocolFamily.UNIX)
        println("Connecting to ${sockFile.path}")

        while(true) {
            val line = readln()
            if (line.isEmpty())
                break

            println("doing action based on: $line")
        }
    }
}