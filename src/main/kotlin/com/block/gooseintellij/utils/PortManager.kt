package com.block.gooseintellij.utils

import com.block.gooseintellij.config.GooseChatEnvironment
import com.intellij.openapi.diagnostic.Logger
import java.io.IOException
import java.net.ServerSocket
import java.net.InetSocketAddress
import java.net.DatagramSocket
import java.util.concurrent.ConcurrentHashMap

object PortManager {
    private val logger = Logger.getInstance(PortManager::class.java)
    private val usedPorts = ConcurrentHashMap.newKeySet<Int>()
    private const val SOCKET_TIMEOUT_MS = 200 // 200ms timeout for quick checks

    fun findAvailablePort(): Int {
        for (port in GooseChatEnvironment.minPort..GooseChatEnvironment.maxPort) {
            if (!usedPorts.contains(port) && isPortAvailable(port)) {
                usedPorts.add(port)
                // Double check after adding to prevent race conditions
                if (isPortAvailable(port)) {
                    logger.debug("Found available port: $port")
                    return port
                }
                usedPorts.remove(port)
            }
        }
        throw IOException("No available ports in range ${GooseChatEnvironment.minPort}-${GooseChatEnvironment.maxPort}")
    }

    fun releasePort(port: Int) {
        usedPorts.remove(port)
        logger.debug("Released port: $port")
    }

    fun isPortAvailable(port: Int): Boolean {
        if (port < 0 || port > 65535) {
            return false
        }

        var tcpSocket: ServerSocket? = null
        var udpSocket: DatagramSocket? = null

        try {
            // Try to bind TCP socket
            tcpSocket = ServerSocket()
            tcpSocket.reuseAddress = false
            tcpSocket.soTimeout = SOCKET_TIMEOUT_MS
            tcpSocket.bind(InetSocketAddress("localhost", port))

            // Try to bind UDP socket
            udpSocket = DatagramSocket(null)
            udpSocket.reuseAddress = false
            udpSocket.soTimeout = SOCKET_TIMEOUT_MS
            udpSocket.bind(InetSocketAddress("localhost", port))

            // If both bindings succeed, the port is available
            return true

        } catch (e: IOException) {
            logger.debug("Port $port is not available: ${e.message}")
            return false

        } catch (e: Exception) {
            logger.warn("Unexpected error checking port $port availability", e)
            return false

        } finally {
            // Clean up resources
            try {
                tcpSocket?.close()
            } catch (e: Exception) {
                logger.debug("Error closing TCP socket", e)
            }

            try {
                udpSocket?.close()
            } catch (e: Exception) {
                logger.debug("Error closing UDP socket", e)
            }
        }
    }
}