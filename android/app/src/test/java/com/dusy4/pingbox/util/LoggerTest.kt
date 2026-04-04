package com.dusy4.pingbox.util

import android.content.Context
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class LoggerTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        Logger.init(context)
        Logger.clearLogs()
    }

    @Test
    fun `log entry stores correct data`() {
        // Clear and start fresh
        Logger.clearLogs()
        
        Logger.i("TestTag", "Test message")
        
        // Access logs directly through getLogsByLevel
        val logs = Logger.getLogsByLevel(LogLevel.INFO)
        // Should have at least our test message (may also have "Logs cleared" from clearLogs())
        assertTrue("Expected at least 1 log, got ${logs.size}", logs.size >= 1)
        // Find our specific log entry
        val testLog = logs.find { it.tag == "TestTag" && it.message == "Test message" }
        assertNotNull("Test log entry not found", testLog)
        assertEquals(LogLevel.INFO, testLog?.level)
    }

    @Test
    fun `logs are limited to max entries`() {
        Logger.clearLogs()
        
        // Add more than MAX_LOG_ENTRIES (1000)
        repeat(1100) { i ->
            Logger.d("Tag", "Message $i")
        }
        
        val logs = Logger.getLogsByLevel(LogLevel.VERBOSE)
        assertTrue(logs.size <= 1000)
    }

    @Test
    fun `clear logs removes all entries`() {
        Logger.clearLogs()
        
        Logger.i("Tag", "Message 1")
        Logger.i("Tag", "Message 2")
        
        var logs = Logger.getLogsByLevel(LogLevel.INFO)
        // Should have our 2 messages plus "Logs cleared" from clearLogs()
        val userLogs = logs.filter { it.tag == "Tag" }
        assertEquals("Expected 2 user logs", 2, userLogs.size)
        
        Logger.clearLogs()
        
        // After clear, only the "Logs cleared" message should remain
        logs = Logger.getLogsByLevel(LogLevel.INFO)
        assertEquals(1, logs.size)
        assertEquals("Logger", logs[0].tag)
        assertEquals("Logs cleared", logs[0].message)
    }

    @Test
    fun `debug mode toggle persists`() {
        Logger.setDebugMode(true)
        assertTrue(Logger.isDebugMode())
        
        Logger.setDebugMode(false)
        assertFalse(Logger.isDebugMode())
    }

    @Test
    fun `getLogsByLevel filters correctly`() {
        Logger.v("Tag", "Verbose")
        Logger.d("Tag", "Debug")
        Logger.i("Tag", "Info")
        Logger.w("Tag", "Warn")
        Logger.e("Tag", "Error")
        
        val warnAndAbove = Logger.getLogsByLevel(LogLevel.WARN)
        assertEquals(2, warnAndAbove.size)
        assertTrue(warnAndAbove.all { it.level.ordinal >= LogLevel.WARN.ordinal })
    }

    @Test
    fun `log entry format includes all data`() {
        val entry = LogEntry(
            level = LogLevel.ERROR,
            tag = "TestTag",
            message = "Error message",
            throwable = RuntimeException("Test exception")
        )
        
        val formatted = entry.format()
        assertTrue(formatted.contains("ERROR"))
        assertTrue(formatted.contains("TestTag"))
        assertTrue(formatted.contains("Error message"))
        assertTrue(formatted.contains("RuntimeException"))
    }

    @Test
    fun `exportLogs respects filters`() {
        Logger.d("Alpha", "Debug message")
        Logger.i("Beta", "Info message")
        Logger.e("Alpha", "Error message")
        
        val exported = Logger.exportLogs(minLevel = LogLevel.ERROR, filterTag = "Alpha")
        assertTrue(exported.contains("Error message"))
        assertFalse(exported.contains("Debug message"))
        assertFalse(exported.contains("Beta"))
    }

    @Test
    fun `safeExecute returns null on exception`() {
        val result = safeExecute("Test", "operation") {
            throw RuntimeException("Test error")
        }
        
        assertNull(result)
    }

    @Test
    fun `safeExecute returns value on success`() {
        val result = safeExecute("Test", "operation") {
            "success"
        }
        
        assertEquals("success", result)
    }

    @Test
    fun `safeExecuteWithDefault returns default on exception`() {
        val result = safeExecuteWithDefault("Test", "operation", "default") {
            throw RuntimeException("Test error")
        }
        
        assertEquals("default", result)
    }
}
