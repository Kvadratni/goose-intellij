package com.block.gooseintellij

import java.io.BufferedWriter
import java.io.OutputStreamWriter

object GooseCommandHelper {
    private var process: Process? = null
    private var writer: BufferedWriter? = null

    fun startGooseSession() {
        if (process == null || !process!!.isAlive) {
            process = ProcessBuilder("goose", "session start").start()
            writer = BufferedWriter(OutputStreamWriter(process!!.outputStream))
        }
    }

    fun sendCommandToGoose(command: String): String {
        return try {
            if (process == null || !process!!.isAlive) {
                startGooseSession()
            }
            writer?.write(command)
            writer?.newLine()
            writer?.flush()

            val reader = process!!.inputStream.bufferedReader()
            val output = reader.readText()
            output
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }
}
