package me.lucat1.sock

import io.klogging.Level
import io.klogging.config.loggingConfiguration
import io.klogging.rendering.RENDER_ANSI
import io.klogging.sending.STDOUT
import kotlinx.coroutines.coroutineScope
import me.lucat1.sock.reader.CLIENT_USAGE
import me.lucat1.sock.reader.Client
import me.lucat1.sock.server.Server
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
    
Arguments:
    socket_path     The path to the Unix Domain Socket where the program will bind and listen for messages.
    
Description:
    Opens a connection to the specified UNIX socket and allows to send commands interactively.
$CLIENT_USAGE
"""

suspend fun main(args: Array<String>): Unit = coroutineScope {
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

            // Setup the klogging structured logging library (only on the server)
            loggingConfiguration {
                sink("stdout", RENDER_ANSI, STDOUT)
                logging {
                    fromMinLevel(Level.DEBUG) {
                        toSink("stdout")
                    }
                }
            }

            val socketPath = args[1]
            val filePath = args[2]

            Server(socketPath, filePath).run()
        }
        "client" -> {
            if (args.size < 2) {
                print(clientUsage)
                exitProcess(4)
            }

            val socketPath = args[1]

            Client(socketPath).run()
        }
        else -> {
            println("Invalid subcommand: $subcommand")
            exitProcess(2)
        }
    }
}
