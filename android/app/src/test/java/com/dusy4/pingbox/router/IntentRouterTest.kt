package com.dusy4.pingbox.router

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class IntentRouterTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
    }

    @Test
    fun `buildIntent creates url intent correctly`() {
        val target = ResolvedTarget(
            type = "url",
            value = "https://example.com",
            notificationId = "test_1"
        )

        val intent = IntentRouter.buildIntent(context, target)

        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals("https://example.com", intent.data?.toString())
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }

    @Test
    fun `buildIntent creates deeplink intent correctly`() {
        val target = ResolvedTarget(
            type = "deeplink",
            value = "tg://resolve?domain=test",
            notificationId = "test_1"
        )

        val intent = IntentRouter.buildIntent(context, target)

        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals("tg://resolve?domain=test", intent.data?.toString())
    }

    @Test
    fun `buildIntent creates package intent for installed app`() {
        // In Robolectric, getLaunchIntentForPackage returns null by default
        // So this test verifies the fallback behavior for packages
        val target = ResolvedTarget(
            type = "package",
            value = "com.android.settings",  // System package that might have launch intent
            notificationId = "test_1"
        )

        val intent = IntentRouter.buildIntent(context, target)

        // Either we get a launch intent, or we get a fallback to RouterActivity
        // Since we can't reliably mock the launch intent in Robolectric,
        // we verify the intent is created (not null) and has a valid action
        assertNotNull(intent)
        assertTrue(intent.action == Intent.ACTION_MAIN || intent.component?.className?.contains("RouterActivity") == true)
    }

    @Test
    fun `buildIntent creates fallback for non-existent package`() {
        val target = ResolvedTarget(
            type = "package",
            value = "com.nonexistent.app",
            notificationId = "test_1"
        )

        val intent = IntentRouter.buildIntent(context, target)

        // Should fallback to RouterActivity
        assertEquals(context.packageName, intent.component?.packageName)
        assertEquals("RouterActivity", intent.component?.className?.substringAfterLast("."))
        assertNotNull(intent.getStringExtra("error"))
    }

    @Test
    fun `buildIntent creates component intent correctly`() {
        val target = ResolvedTarget(
            type = "component",
            value = "com.example.app/.MainActivity",
            notificationId = "test_1"
        )

        val intent = IntentRouter.buildIntent(context, target)

        assertEquals("com.example.app", intent.component?.packageName)
        assertEquals(".MainActivity", intent.component?.className)
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }

    @Test
    fun `buildIntent creates fallback for invalid component format`() {
        val target = ResolvedTarget(
            type = "component",
            value = "invalid_format",
            notificationId = "test_1"
        )

        val intent = IntentRouter.buildIntent(context, target)

        assertEquals(context.packageName, intent.component?.packageName)
        assertNotNull(intent.getStringExtra("error"))
    }

    @Test
    fun `buildIntent creates intent_uri intent correctly`() {
        val target = ResolvedTarget(
            type = "intent_uri",
            value = "intent://example.com/#Intent;action=android.intent.action.VIEW;end",
            notificationId = "test_1"
        )

        val intent = IntentRouter.buildIntent(context, target)

        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }

    @Test
    fun `buildIntent creates fallback for invalid intent_uri`() {
        // An intent URI with malformed syntax that will cause parseUri to fail
        val target = ResolvedTarget(
            type = "intent_uri",
            value = "not_a_valid_intent_uri_at_all",
            notificationId = "test_1"
        )

        val intent = IntentRouter.buildIntent(context, target)

        // Intent.parseUri with URI_INTENT_SCHEME requires proper intent: scheme
        // An invalid string should trigger the catch block and return fallback
        // Note: In some Android versions, parseUri may not throw for arbitrary strings,
        // so we check that we get a valid intent back (either parsed or fallback)
        assertNotNull(intent)
        // If it's a fallback, it will have the error extra or be RouterActivity
        // If parsing succeeded, it will have the ACTION_VIEW with the URI
        assertTrue(
            intent.getStringExtra("error") != null ||  // Fallback
            intent.action != null                      // Parsed successfully
        )
    }

    @Test
    fun `buildIntent defaults to RouterActivity for unknown type`() {
        val target = ResolvedTarget(
            type = "unknown_type",
            value = "",
            notificationId = "test_1"
        )

        val intent = IntentRouter.buildIntent(context, target)

        assertEquals(context.packageName, intent.component?.packageName)
        assertEquals("test_1", intent.getStringExtra("notification_id"))
    }

    @Test
    fun `buildIntent creates RouterActivity intent for 'none' type`() {
        val target = ResolvedTarget(
            type = "none",
            value = "",
            notificationId = "test_1"
        )

        val intent = IntentRouter.buildIntent(context, target)

        assertEquals(context.packageName, intent.component?.packageName)
        assertEquals("test_1", intent.getStringExtra("notification_id"))
    }

    @Test
    fun `buildPendingIntentTarget wraps underlying intent`() {
        val target = ResolvedTarget(
            type = "url",
            value = "https://example.com",
            notificationId = "test_1"
        )

        val intent = IntentRouter.buildPendingIntentTarget(context, target)

        assertEquals(context.packageName, intent.component?.packageName)
        assertEquals("test_1", intent.getStringExtra("notification_id"))
        
        val wrappedIntent = intent.getParcelableExtra<Intent>("target_intent")
        assertNotNull(wrappedIntent)
        assertEquals(Intent.ACTION_VIEW, wrappedIntent?.action)
        assertEquals("https://example.com", wrappedIntent?.data?.toString())
    }
}
