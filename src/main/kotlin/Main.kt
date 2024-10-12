package me.lucat1.sock

import me.lucat1.sock.reader.Server
import kotlin.system.exitProcess

const val usage = """Usage: sock subcommand [args...]

Subcommands:
    server          Connects to the specified socket and executes operations on `file_path`.
    client          Connects to the specified socket and writes operations to be performed.
"""

const val serverUsage = """Usage: sock server <socket_path> <file_path>
   
Arguments:
    socket_path     The path to the Unix Domain Socket where the program will bind and listen for messages.
    file_path       The file to which content should be written or cleared based on the message type.
"""

const val clientUsage = """Usage: sock client <socket_path>
Description:
    Opens a connection to the specified UNIX socket and allows to send commands interactively
    The syntax for a command is:
        <Message Type>  <Data>
    Exampels:
        2 "hello world"       # append hello world to the output file
        3                     # clear the output file
        5                     # send a ping
   
Arguments:
    socket_path     The path to the Unix Domain Socket where the program will bind and listen for messages.
"""

fun main(args: Array<String>) {
    /*
     * Note that, as opposed to standard UNIX practice, args[0] is not the program name.
     * This is the observed behaviour when running with Gradle.
     */
    if (args.isEmpty()) {
        println(usage)
        exitProcess(1)
    }

    when (val subcommand = args[0]) {
        "server" -> {
            if (args.size < 3) {
                print(serverUsage)
                exitProcess(3)
            }
            val socketPath = args[1]
            val filePath = args[2]
            val server = Server(socketPath, filePath)
            server.run()
        }
        "client" -> {
            if (args.size < 2) {
                print(clientUsage)
                exitProcess(4)
            }
        }
        else -> {
            println("Invalid subcommand: $subcommand")
            exitProcess(2)
        }
    }
}
