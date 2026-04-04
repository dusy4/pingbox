package com.dusy4.pingbox

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast

class RouterActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val targetIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("target_intent", Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("target_intent")
        }

        if (targetIntent == null) {
            val error = intent.getStringExtra("error")
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
            }
            finish()
            return
        }

        try {
            startActivity(targetIntent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "App not installed", Toast.LENGTH_SHORT).show()
            finish()
        } catch (e: SecurityException) {
            Toast.makeText(this, "Activity not accessible (not exported)", Toast.LENGTH_SHORT).show()
            val fallback = packageManager.getLaunchIntentForPackage(packageName)
            fallback?.let { startActivity(it) }
        } finally {
            finish()
        }
    }
}
