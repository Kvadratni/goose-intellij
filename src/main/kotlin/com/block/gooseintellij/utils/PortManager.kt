package com.block.gooseintellij.utils

import com.block.gooseintellij.config.GooseChatEnvironment
import com.intellij.openapi.diagnostic.Logger
import java.io.IOException
import java.net.ServerSocket
import java.util.concurrent.ConcurrentHashMap

object PortManager {
    private val logger = Logger.getInstance(PortManager::class.java)
    private val usedPorts = ConcurrentHashMap.newKeySet<Int>()

    fun findAvailablePort(): Int {
        for (port in GooseChatEnvironment.minPort..GooseChatEnvironment.maxPort) {
            if (!usedPorts.contains(port) && isPortAvailable(port)) {
                usedPorts.add(port)
                return port
            }
        }
        throw IOException("No available ports in range ${GooseChatEnvironment.minPort}-${GooseChatEnvironment.maxPort}")
    }

    fun releasePort(port: Int) {
        usedPorts.remove(port)
    }

    private fun isPortAvailable(port: Int): Boolean {
        return try {
            ServerSocket(port).use { true }
        } catch (e: IOException) {
            false
        }
    }
}