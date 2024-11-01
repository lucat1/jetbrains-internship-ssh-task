package me.lucat1.sock.server

import io.klogging.Klogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.nio.file.Path

enum class WriterAction {
    Write,
    Clear,
}

class WriterMessage(val action: WriterAction,
                    val content: String?,
                    val ackChannel: Channel<Throwable?>) { }

class Writer(private val outputPath: Path, private val chan: Channel<WriterMessage>, private var logger: Klogger) {
    val file = File(outputPath.toString())

    suspend fun write() = withContext(Dispatchers.IO) {
        if (!file.exists()) {
            file.createNewFile()
            logger.info("Created new file {filePath}", outputPath)
        }

        while (true) {
            val msg = chan.receive()
            var res: Throwable?
            try {
                when (msg.action) {
                    WriterAction.Write -> {
                        assert(msg.content != null)
                        FileOutputStream(file, true).bufferedWriter().use { writer ->
                            writer.write(msg.content as String)
                            logger.info("Appended {bytes} characters to {file}", msg.content.length, file)
                        }
                    }

                    WriterAction.Clear -> {
                        FileWriter(file).use { writer ->
                            writer.write("")
                            logger.info("Cleared {file}", file)
                        }
                    }
                }
                res = null
            } catch (e: Exception) {
                res = e
            }
            msg.ackChannel.send(res)
        }
    }
}