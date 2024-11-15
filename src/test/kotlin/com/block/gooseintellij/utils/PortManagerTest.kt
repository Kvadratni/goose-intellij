package com.block.gooseintellij.utils

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.IOException
import java.net.ServerSocket

class PortManagerTest {
    @Test
    fun testFindAvailablePort() {
        val port = PortManager.findAvailablePort()
        assertTrue(port in 1024..65535)
        
        // Port should be marked as used
        assertFalse(PortManager.isPortAvailable(port))
        
        // Release and verify it becomes available
        PortManager.releasePort(port)
        assertTrue(PortManager.isPortAvailable(port))
    }
    
    @Test
    fun testPortUnavailableWhenInUse() {
        var serverSocket: ServerSocket? = null
        try {
            // Bind a server socket to a port
            serverSocket = ServerSocket(0) // Let OS choose port
            val usedPort = serverSocket.localPort
            
            // Verify PortManager detects it as unavailable
            assertFalse(PortManager.isPortAvailable(usedPort))
                
            // Verify findAvailablePort skips this port
            val newPort = PortManager.findAvailablePort()
            assertNotEquals(usedPort, newPort)
        } finally {
            serverSocket?.close()
        }
    }
    
    @Test
    fun testConcurrentPortAllocation() {
        val threads = (1..10).map { 
            Thread {
                try {
                    val port = PortManager.findAvailablePort()
                    assertTrue(port in 1024..65535)
                    Thread.sleep(100) // Simulate some work
                    PortManager.releasePort(port)
                } catch (e: IOException) {
                    fail("Port allocation failed: ${e.message}")
                }
            }
        }
        
        threads.forEach { it.start() }
        threads.forEach { it.join() }
    }
}
