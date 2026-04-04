package com.dusy4.pingbox.push

import com.dusy4.pingbox.router.ResolvedTarget
import org.junit.Assert.*
import org.junit.Test

class RuleEngineTest {

    @Test
    fun `resolve returns default target when no rules match`() {
        val payload = NotificationPayload(
            id = "test_1",
            title = "Test",
            body = "",
            icon = null,
            tag = "no_match",
            targetType = "url",
            targetValue = "https://example.com"
        )
        
        val rules = emptyList<LocalRule>()
        
        val result = RuleEngine.resolve(payload, rules)
        
        assertEquals("url", result.type)
        assertEquals("https://example.com", result.value)
        assertEquals("test_1", result.notificationId)
    }

    @Test
    fun `resolve matches exact tag`() {
        val payload = NotificationPayload(
            id = "test_1",
            title = "Test",
            body = "",
            icon = null,
            tag = "ci_build",
            targetType = "url",
            targetValue = "https://example.com"
        )
        
        val rules = listOf(
            LocalRule(
                id = "rule_1",
                name = "CI Rule",
                matchTag = "ci_build",
                targetType = "package",
                targetValue = "com.github.android",
                priority = 1,
                enabled = true
            )
        )
        
        val result = RuleEngine.resolve(payload, rules)
        
        assertEquals("package", result.type)
        assertEquals("com.github.android", result.value)
    }

    @Test
    fun `resolve matches wildcard tag`() {
        val payload = NotificationPayload(
            id = "test_1",
            title = "Test",
            body = "",
            icon = null,
            tag = "ci_fail",
            targetType = "url",
            targetValue = "https://example.com"
        )
        
        val rules = listOf(
            LocalRule(
                id = "rule_1",
                name = "CI Rule",
                matchTag = "ci_*",
                targetType = "package",
                targetValue = "com.github.android",
                priority = 1,
                enabled = true
            )
        )
        
        val result = RuleEngine.resolve(payload, rules)
        
        assertEquals("package", result.type)
        assertEquals("com.github.android", result.value)
    }

    @Test
    fun `resolve respects priority order`() {
        val payload = NotificationPayload(
            id = "test_1",
            title = "Test",
            body = "",
            icon = null,
            tag = "alert",
            targetType = "url",
            targetValue = "https://example.com"
        )
        
        val rules = listOf(
            LocalRule(
                id = "rule_1",
                name = "Low Priority",
                matchTag = "alert",
                targetType = "package",
                targetValue = "com.low.app",
                priority = 10,
                enabled = true
            ),
            LocalRule(
                id = "rule_2",
                name = "High Priority",
                matchTag = "alert",
                targetType = "package",
                targetValue = "com.high.app",
                priority = 1,
                enabled = true
            )
        )
        
        val result = RuleEngine.resolve(payload, rules)
        
        // High priority (lower number) should win
        assertEquals("com.high.app", result.value)
    }

    @Test
    fun `resolve ignores disabled rules`() {
        val payload = NotificationPayload(
            id = "test_1",
            title = "Test",
            body = "",
            icon = null,
            tag = "ci_build",
            targetType = "url",
            targetValue = "https://example.com"
        )
        
        val rules = listOf(
            LocalRule(
                id = "rule_1",
                name = "Disabled Rule",
                matchTag = "ci_build",
                targetType = "package",
                targetValue = "com.disabled.app",
                priority = 1,
                enabled = false
            )
        )
        
        val result = RuleEngine.resolve(payload, rules)
        
        // Should use default target since rule is disabled
        assertEquals("url", result.type)
        assertEquals("https://example.com", result.value)
    }

    @Test
    fun `resolve handles empty tag pattern`() {
        val payload = NotificationPayload(
            id = "test_1",
            title = "Test",
            body = "",
            icon = null,
            tag = "anything",
            targetType = "url",
            targetValue = "https://example.com"
        )
        
        val rules = listOf(
            LocalRule(
                id = "rule_1",
                name = "Empty Pattern",
                matchTag = "",
                targetType = "package",
                targetValue = "com.test.app",
                priority = 1,
                enabled = true
            )
        )
        
        val result = RuleEngine.resolve(payload, rules)
        
        // Empty pattern should not match
        assertEquals("url", result.type)
        assertEquals("https://example.com", result.value)
    }

    @Test
    fun `resolve handles complex wildcard pattern`() {
        val payload = NotificationPayload(
            id = "test_1",
            title = "Test",
            body = "",
            icon = null,
            tag = "grafana_alert_critical",
            targetType = "url",
            targetValue = "https://example.com"
        )
        
        val rules = listOf(
            LocalRule(
                id = "rule_1",
                name = "Grafana Rule",
                matchTag = "grafana_*_critical",
                targetType = "deeplink",
                targetValue = "grafana://alerts",
                priority = 1,
                enabled = true
            )
        )
        
        val result = RuleEngine.resolve(payload, rules)
        
        assertEquals("deeplink", result.type)
        assertEquals("grafana://alerts", result.value)
    }
}
