# PingBox (TriggerApp)

**Universal Notification Router for Android** — Send push notifications from any service and route them to open exactly the app, URL, or activity you want. No middleman UI.

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Python](https://img.shields.io/badge/python-3.12+-blue.svg)](https://www.python.org/downloads/)
[![Kotlin](https://img.shields.io/badge/kotlin-1.9.21-blue.svg)](https://kotlinlang.org/)
[![FastAPI](https://img.shields.io/badge/FastAPI-0.109+-009688.svg)](https://fastapi.tiangolo.com/)
[![Android](https://img.shields.io/badge/Android-8.0%2B-brightgreen.svg)](https://developer.android.com/)

## App Screenshots

<p align="center">
  <img src="./WhatsApp%20Image%202026-05-08%20at%2023.17.28.jpeg" width="220" alt="PingBox screenshot 1" />
  <img src="./WhatsApp%20Image%202026-05-08%20at%2023.17.28%20(1).jpeg" width="220" alt="PingBox screenshot 2" />
  <img src="./WhatsApp%20Image%202026-05-08%20at%2023.17.28%20(2).jpeg" width="220" alt="PingBox screenshot 3" />
  <img src="./WhatsApp%20Image%202026-05-08%20at%2023.17.28%20(3).jpeg" width="220" alt="PingBox screenshot 4" />
</p>

---

## Quick Start

```bash
# 1. Clone the repo
git clone https://github.com/your-org/pingbox.git
cd pingbox

# 2. Configure environment
cp .env.example .env
# Edit .env with your secrets (DB password, Firebase JSON, JWT secret)

# 3. Start the backend
python bootstrap.py redeploy

# 4. Build the Android app
cd android && ./gradlew assembleDebug
```

**One-liner test** (after backend is running):
```bash
curl -X POST https://your-server/v1/push \
  -H "Authorization: Bearer YOUR_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"title":"Hello","body":"It works!","target":{"type":"none"}}'
```

---

## Table of Contents

### For Users
1. [What is PingBox?](#1-what-is-pingbox)
2. [Key Features](#2-key-features)
3. [How It Works — High-Level Flow](#3-how-it-works--high-level-flow)
4. [Getting Started](#4-getting-started)
5. [App Interface](#5-app-interface)
6. [Settings Tab](#6-settings-tab)
7. [Rules Tab](#7-rules-tab)
8. [History Tab](#8-history-tab)
9. [How Notifications Work](#9-how-notifications-work)
10. [API Usage](#10-api-usage)
11. [Target Types Reference](#11-target-types-reference)
12. [Troubleshooting (User)](#12-troubleshooting-user)
13. [Privacy](#13-privacy)

### For Developers
14. [Architecture Overview](#14-architecture-overview)
15. [Tech Stack](#15-tech-stack)
16. [Backend Architecture](#16-backend-architecture)
17. [Android App Architecture](#17-android-app-architecture)
18. [Database Models](#18-database-models)
19. [API Reference — All Endpoints](#19-api-reference--all-endpoints)
20. [Services (FCM, Rate Limiting, Webhook)](#20-services)
21. [Android Components](#21-android-components)
22. [Data Flow Diagrams](#22-data-flow-diagrams)
23. [Security](#23-security)
24. [Development Setup](#24-development-setup)
25. [Environment Variables Reference](#25-environment-variables-reference)
26. [Container Management (bootstrap.py)](#26-container-management-bootstrappy)
27. [Deployment](#27-deployment)
28. [Testing](#28-testing)
29. [Edge Cases & Error Handling](#29-edge-cases--error-handling)
30. [Android Permissions](#30-android-permissions)
31. [Debug & Logging System](#31-debug--logging-system)
32. [Unit Testing](#32-unit-testing)
33. [Future Enhancements](#33-future-enhancements)
34. [Development Roadmap](#34-development-roadmap)
35. [Licensing](#35-licensing)

### Appendix
- [Common Deep Link URI Schemes](#appendix-a-common-deep-link-uri-schemes)
- [Webhook Integration Templates](#appendix-b-webhook-integration-templates)
- [Local Rule Example](#appendix-c-local-rule-example)
- [Bootstrap.py Full Reference](#appendix-d-bootstrappy-full-reference)

---

## 1. What is PingBox?

PingBox (also known as TriggerApp) is a **universal notification router for Android**. Any event source — a bash script on your PC, a Python job, a GitHub Actions run, a Grafana alert, an n8n workflow, a cURL call — can send a push notification to your Android device via a simple HTTP POST.

The differentiating feature: **when you tap the notification, it does NOT open PingBox**. Instead, it opens exactly the app, tab, URL, or activity you configured — Chrome on a specific URL, your Telegram chat, your Instagram profile tab, a GitHub Actions run page, or any other explicitly targeted destination.

This solves a real pain point: existing push notification tools (ntfy, Pushover, Pushbullet) only open a URL in a browser. PingBox can open **any exported Android activity, any deep-link URI scheme, any app's launch screen**, without root or Shizuku.

### What Makes PingBox Different

| Tool | Delivers Push? | Opens Custom URL? | Opens Specific App Tab? | Opens Explicit Activity? | No Middleman UI? |
|------|---------------|-------------------|------------------------|--------------------------|-----------------|
| ntfy | ✅ | ✅ (browser only) | ❌ | ❌ | ❌ |
| Pushover | ✅ | ✅ (browser only) | ❌ | ❌ | ❌ |
| Pushbullet | ✅ | ✅ (browser only) | ❌ | ❌ | ❌ |
| **PingBox** | ✅ | ✅ | ✅ | ✅ (if exported) | ✅ |

### Core Principles

- **Core routing is always free, instant, and unlimited.**
- **No visible PingBox UI on notification tap** — the OS fires the PendingIntent directly at the target. PingBox is invisible at runtime.
- **Self-hostable backend** — included in the repo, deploy with Docker Compose.
- **Works without an account** — use PingBox entirely locally, no login required.

---

## 2. Key Features

- **Instant Push Delivery** — Notifications arrive via Firebase Cloud Messaging (FCM) in under 3 seconds
- **Smart Routing** — Tap a notification to open any URL, deep link, app, or specific activity
- **Rule Engine** — Define rules to automatically route notifications based on tags (supports wildcards like `ci_*`)
- **Local History** — All received notifications stored locally in a Room database (persists across restarts)
- **FCM Push Notifications** — Handles push events so triggers arrive immediately, even when the app is killed
- **Device Registration** — Syncs FCM tokens via WorkManager to ensure delivery is routed to the right device
- **Multi-Target Support** — `url`, `deeplink`, `package`, `component`, `intent_uri`, and `none` target types
- **UnifiedPush Support** — Works on de-Googled devices without Google Play Services
- **Boot Recovery** — Auto-re-registers device FCM token after reboot
- **Self-Hostable** — Full Docker Compose deployment for complete data sovereignty

---

## 3. How It Works — High-Level Flow

```
┌─────────────────────────────────────────────────────────────────┐
│                        EVENT SOURCES                            │
│  bash script │ Python │ n8n │ Make │ Grafana │ GitHub Actions   │
│  cURL │ Zapier │ IFTTT │ Home Assistant │ any HTTP client       │
└──────────────────────────┬──────────────────────────────────────┘
                           │ HTTPS POST  /v1/push
                           ▼
┌──────────────────────────────────────────────────────────────────┐
│                    PINGBOX BACKEND                               │
│                                                                  │
│  Auth & Rate Limit → Validate Payload → Store in History        │
│         → Dispatch to FCM HTTP v1 API                           │
│                                                                  │
│  Stack: FastAPI + PostgreSQL 16 + Redis 7 + Firebase Admin SDK  │
└──────────────────────────┬───────────────────────────────────────┘
                           │ FCM data-only message
                           ▼
┌──────────────────────────────────────────────────────────────────┐
│                 ANDROID CLIENT                                   │
│                                                                  │
│  FirebaseMessagingService.onMessageReceived()                    │
│    → RuleEngine.resolve(payload, localRules)                     │
│    → IntentRouter.buildIntent(target_type, target_value)         │
│    → NotificationManager.notify(PendingIntent → target)         │
│                                                                  │
│  [user taps notification]                                        │
│                                                                  │
│  OS fires PendingIntent → TARGET APP opens directly             │
│  (no PingBox UI ever shown)                                     │
└──────────────────────────────────────────────────────────────────┘
```

### The No-Middleman Guarantee

The PendingIntent inside the notification wraps the **target's Intent directly**, not PingBox's own Activity. When the user taps:
1. OS fires `PendingIntent`
2. Intent resolves to target (Chrome, Instagram, Telegram, etc.)
3. Target app opens at the correct screen
4. PingBox code never executes in the foreground

If the intent fails (app not installed, activity not exported), a transparent `RouterActivity` (invisible, zero-UI theme) catches the exception, shows a Toast, and optionally falls back.

---

## 4. Getting Started

### Prerequisites

1. Android device running Android 8.0 (API 26) or higher
2. A server running the PingBox backend API (or use the hosted version)
3. Optional: Firebase Cloud Messaging (FCM) setup for push notifications

### Initial Setup

1. **Install the app** on your Android device (APK or from Play Store)
2. **Configure the server URL** in Settings
   - For local development: `http://YOUR_PC_IP:8080/`
   - For production: Your public server URL (e.g., `https://your-domain.com/`)
   - *Note: If you omit the `http://` or `https://` prefix, the app auto-defaults to `https://`*
3. **Create an account or login** — tap "Not logged in" in Settings
4. **Generate an API key** — tap "Generate New Key" after logging in
5. **Register your device** — happens automatically when logged in

---

## 5. App Interface

The app uses a bottom navigation with 3 tabs:

```
┌─────────────────────────────────┐
│  [Settings]  [Rules]  [History]│
├─────────────────────────────────┤
│                                 │
│                                 │
│      (Content Area)             │
│                                 │
│                                 │
│                                 │
└─────────────────────────────────┘
```

The UI is built with **100% Jetpack Compose** using Material 3 dark theme design patterns.

---

## 6. Settings Tab

The Settings tab is where you configure your account and connection.

### Fields

| Field | Description |
|-------|-------------|
| **Auth Status** | Shows if you're logged in and as which email (green dot = active) |
| **Login/Logout** | Tap "Not logged in" to open the auth dialog |
| **Server URL** | URL of the PingBox backend API (read-only, configured in code) |
| **Active API Key** | Your API key for sending notifications (long-press to copy) |
| **Device Identifier** | Friendly name for this device |
| **Quick Actions** | Copy API Key, Generate New Key, Log Out |
| **Save Configuration** | Persists device name changes |

### Authentication

The auth dialog supports both **Login** and **Register** modes:
- **Email** — validated with Android's `Patterns.EMAIL_ADDRESS`
- **Password** — minimum 8 characters, with show/hide toggle
- Error messages displayed in a red banner within the dialog

### API Key Management

- **Copy API Key** — Quick action button or long-press on the API key row
- **Generate New Key** — Creates a new `tk_...` key via the backend API
- Keys are auto-generated after login/register
- Previous session keys are detected and cleared on login

---

## 7. Rules Tab

Rules define how incoming notifications are routed based on their `tag`.

### Viewing Rules

The rules list shows all your configured rules with:
- Rule name
- Match tag pattern (with wildcard support)
- Target type and value
- Edit/Delete buttons
- Enabled/disabled status

### Creating a Rule

1. Tap the **+** (FAB) button
2. Fill in the rule details:

| Field | Description | Example |
|-------|-------------|---------|
| **Rule Name** | Friendly name | "Open YouTube" |
| **Match Tag** | Tag to match (supports `*` wildcard) | `video`, `alert*`, `*` |
| **Target Type** | Action type | See Target Types below |
| **Target Value** | Value for the target | `https://youtube.com` |
| **Priority** | Lower = higher priority | `0`, `1`, `2` |
| **Enabled** | Whether rule is active | Checkbox |

3. Tap **Save**

### Rule Matching

When a notification arrives:
1. All enabled rules are sorted by priority (lower = higher priority)
2. Rules are checked in order for tag match
3. First matching rule's target is used
4. If no rule matches, the notification's default target is used

### Wildcard Matching

The `*` character matches any sequence of characters:
- `alert*` matches `alert_promo`, `alert_critical`, `alert_123`
- `ci_*` matches `ci_fail`, `ci_pass`, `ci_deploy`
- `*` matches everything (catch-all rule)

### Examples

**Route all "alert" notifications to Slack:**
```
Match Tag: alert
Target Type: deeplink
Target Value: slack://channel/general
```

**Route YouTube notifications to YouTube app:**
```
Match Tag: video_youtube
Target Type: package
Target Value: com.google.android.youtube
```

**Route promo notifications to browser:**
```
Match Tag: promo*
Target Type: url
Target Value: https://example.com/promo
```

---

## 8. History Tab

The History tab shows a local log of all received notifications.

### Features

- Displays notification title, body, tag, and timestamp
- Persists across app restarts (Room database)
- Shows notifications even when offline
- Sorted by most recent first
- All data stays on-device — never leaves your phone

### Local Storage

All notification history is stored locally in a Room SQLite database. This data never leaves your device.

---

## 9. How Notifications Work

### Full Flow Diagram

```
[External Service] 
      │
      ▼
[PingBox Backend API]
      │ POST /v1/push
      ▼
[FCM Server]
      │ FCM Push (data-only message)
      ▼
[PingBox Android App — TriggerMessagingService]
      │ onMessageReceived()
      ▼
[RuleEngine] ───▶ [Local Rules DB (Room)]
      │
      ▼
[Resolved Target]
      │
      ├── url ──────────▶ [Browser opens URL]
      ├── deeplink ─────▶ [App opens via deep link]
      ├── package ──────▶ [App opens by package name]
      ├── component ────▶ [Specific Activity opens]
      ├── intent_uri ───▶ [Custom intent fires]
      └── none ─────────▶ [No action on tap]
```

### FCM Data-Only Messages

PingBox uses **data-only messages exclusively**. This is critical:

- A **notification message** is handled by the FCM SDK automatically when the app is in background — it shows a default notification that always opens the app's launcher Activity. You lose control.
- A **data-only message** always triggers `onMessageReceived()` regardless of app state (foreground, background, killed). Your code runs, builds the notification, sets its own PendingIntent, and you control everything.

FCM payload sent by the backend:
```json
{
  "message": {
    "token": "<device_fcm_token>",
    "data": {
      "ntf_id": "ntf_abc123",
      "title": "CI Build Failed",
      "body": "main branch — commit a3f21c",
      "icon": "⚠️",
      "tag": "ci_fail",
      "priority": "high",
      "target_type": "url",
      "target_value": "https://github.com/user/repo/actions/runs/123"
    },
    "android": { "priority": "high" }
  }
}
```

**No `notification` key. Only `data`.** This is mandatory.

### Target Resolution

1. App receives notification via FCM
2. Notification payload includes: `title`, `body`, `tag`, `target_type`, `target_value`
3. RuleEngine checks local rules for matching `match_tag`
4. First matching rule's target overrides the default
5. User taps notification
6. IntentRouter builds and executes the intent
7. RouterActivity (transparent) handles errors gracefully

---

## 10. API Usage

### Sending a Notification

```bash
curl -X POST https://your-server/v1/push \
  -H "Authorization: Bearer YOUR_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "New Video",
    "body": "Someone mentioned you",
    "tag": "video_youtube",
    "target": {
      "type": "url",
      "value": "https://youtube.com"
    }
  }'
```

### Response

```json
{
  "id": "ntf_abc123def456",
  "status": "sent",
  "devices_reached": 1
}
```

### Python Example

```python
import requests

requests.post("https://your-server/v1/push",
    headers={"Authorization": "Bearer YOUR_API_KEY"},
    json={
        "title": "Job failed",
        "body": str(e),
        "target": {"type": "package", "value": "com.github.android"}
    })
```

### Push Notification Payload Schema

```json
{
  "title": "Required, max 250 chars",
  "body": "Optional, max 1000 chars",
  "icon": "Optional emoji or icon name",
  "tag": "Optional tag for rule matching",
  "priority": "high|normal",
  "target": {
    "type": "url|deeplink|package|component|intent_uri|none",
    "value": "Target value based on type"
  },
  "devices": ["all"] or ["device_id_1", "device_id_2"]
}
```

---

## 11. Target Types Reference

| Type | Value Example | Description | Root Needed? |
|------|---------------|-------------|--------------|
| `url` | `https://youtube.com` | Opens URL in browser or app that claimed the domain | ❌ |
| `deeplink` | `spotify://track/123` | Opens app via URI scheme, no browser fallback | ❌ |
| `package` | `com.google.android.youtube` | Opens app's launcher activity by package name | ❌ |
| `component` | `com.app/.MainActivity` | Opens specific Activity (must be `exported=true`) | ❌ |
| `intent_uri` | `intent://host#Intent;scheme=...` | Full Android intent URI for maximum flexibility | ❌ |
| `none` | (empty) | Shows notification, no action on tap | ❌ |

**Note**: `component` targets only work if the target Activity has `android:exported="true"`. If not, a `SecurityException` is caught and a fallback is shown.

---

## 12. Troubleshooting (User)

### App Won't Start
- Check Android version (needs 8.0+)
- Reinstall the app
- Check device storage space

### Notifications Not Arriving
1. Verify server URL is correct in Settings
2. Ensure you're logged in
3. Check device has internet connection
4. Verify API key is valid
5. Check notification permission is granted (Android 13+)

### "No API key to copy" Error
- Log in first, then tap "Generate New Key"
- If you see "API key from previous session", log in to generate a fresh key
- The API key is always tied to your account and auto-generated on login

### Rule Not Matching
- Verify tag spelling matches exactly (case-sensitive)
- Check rule is enabled
- Check rule priority (lower number = higher priority)
- Wildcard `*` matches any sequence of characters

### Can't Open Target App
- For `package` type: App must be installed
- For `component` type: Component must exist and be exported
- For `url`/`deeplink`: Valid URL format required

### Server Connection Issues
- Use local IP (e.g., `192.168.1.100`) not `localhost` for local development
- Ensure server is running
- Check firewall allows connection on port 8080
- For mobile access: Use ngrok or deploy to public server

---

## 13. Privacy

- Notification history is stored **locally only** on your device
- No data is sent to third parties except FCM for push delivery
- API key grants full access to your notifications — keep it secure
- Server data is under your control (self-hostable)
- No analytics, no tracking, no telemetry

---

## 14. Architecture Overview

PingBox is a full-stack notification routing system consisting of:
- **Backend**: FastAPI-based Python server for notification management, device registration, and push dispatch
- **Android App**: Native Kotlin app for receiving and routing notifications to target apps

### System Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        PingBox System                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────┐     ┌──────────────────┐     ┌─────────────┐ │
│  │   Client    │     │   PingBox       │     │    FCM      │ │
│  │   (curl)    │────▶│   Backend API    │────▶│   Server    │ │
│  └─────────────┘     │   (FastAPI)      │     └─────────────┘ │
│                      │                  │            │        │
│                      │  ┌────────────┐  │            ▼        │
│                      │  │ PostgreSQL │  │     ┌───────────┐   │
│                      │  └────────────┘  │     │  Android  │   │
│                      │        │         │     │    App    │   │
│                      │  ┌────────────┐  │     └───────────┘   │
│                      │  │    Redis   │  │            │        │
│                      │  └────────────┘  │            ▼        │
│                      └──────────────────┘     ┌───────────┐   │
│                                               │  Router   │   │
│                                               │  Engine   │   │
│                                               └───────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

### Project Structure

```
triggerapp/
├── backend/              ← FastAPI server (Python 3.12)
│   ├── api/              ← HTTP route handlers
│   │   ├── auth.py       ← Auth: register, login, token, me
│   │   ├── keys.py       ← API key CRUD: create, list, revoke
│   │   ├── push.py       ← Push notification sending + batch
│   │   ├── devices.py    ← Device registration, listing, deletion
│   │   ├── history.py    ← Notification history with pagination
│   │   ├── rules.py      ← Notification rules CRUD
│   │   └── webhook.py    ← Webhook receiving + JSONPath mapping
│   ├── core/             ← Core infrastructure
│   │   ├── config.py     ← Pydantic settings, rate limits
│   │   ├── database.py   ← SQLAlchemy async engine + session
│   │   ├── redis.py      ← Async Redis connection pool
│   │   └── security.py   ← Password hashing, JWT, API key gen/verify
│   ├── models/           ← SQLAlchemy ORM models
│   │   ├── user.py       ← User model
│   │   ├── api_key.py    ← APIKey + Device models
│   │   └── notification.py ← NotificationLog, Rule, Webhook
│   ├── services/         ← Service layer
│   │   ├── fcm.py        ← Firebase Cloud Messaging push
│   │   ├── rate_limit.py ← Redis sliding window rate limiting
│   │   └── webhook.py    ← Webhook HMAC signature verification
│   ├── main.py           ← FastAPI app entry point
│   ├── Dockerfile        ← Container build
│   ├── docker-compose.yml ← Production orchestration
│   └── requirements.txt  ← Python dependencies
│
├── android/              ← Kotlin Android app
│   ├── app/src/main/
│   │   ├── java/com/dusy4/pingbox/
│   │   │   ├── TriggerApp.kt            ← Application class
│   │   │   ├── MainActivity.kt          ← Compose entry point
│   │   │   ├── RouterActivity.kt        ← Transparent error handler
│   │   │   ├── BootReceiver.kt          ← Boot-completed receiver
│   │   │   ├── data/
│   │   │   │   ├── local/TriggerDatabase.kt  ← Room DB + DAOs
│   │   │   │   ├── preferences/AppPreferences.kt ← DataStore
│   │   │   │   └── remote/TriggerApiService.kt ← Retrofit API
│   │   │   ├── di/AppModule.kt          ← Koin dependency injection
│   │   │   ├── push/
│   │   │   │   ├── TriggerMessagingService.kt ← FCM + RuleEngine
│   │   │   │   ├── DeviceRegistrationWorker.kt ← WorkManager
│   │   │   │   └── UnifiedPushReceiver.kt ← UnifiedPush broadcast
│   │   │   ├── router/IntentRouter.kt   ← Intent building logic
│   │   │   ├── ui/
│   │   │   │   ├── screens/             ← Compose screens
│   │   │   │   ├── viewmodel/           ← ViewModels
│   │   │   │   ├── navigation/          ← Compose navigation
│   │   │   │   └── theme/               ← Material 3 theme
│   │   │   └── util/                    ← Clipboard, ScreenScale
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
│
├── bootstrap.py          ← Docker container management script
├── .env.example          ← Environment variable template
├── .gitignore
└── README.md             ← This file
```

---

## 15. Tech Stack

| Component | Technology | Version |
|-----------|------------|---------|
| **Backend Runtime** | Python | 3.12 |
| **Backend Framework** | FastAPI | 0.109.2 |
| **ASGI Server** | Uvicorn | 0.27.1 (2 workers) |
| **ORM** | SQLAlchemy | 2.0 (async) |
| **Database** | PostgreSQL | 16 (via asyncpg) |
| **Cache / Rate Limiting** | Redis | 7 (async) |
| **Push Notifications** | Firebase Admin SDK | Latest |
| **Auth** | python-jose (JWT), passlib[bcrypt] | — |
| **Validation** | Pydantic v2 + pydantic-settings | — |
| **Reverse Proxy** | Nginx + Let's Encrypt TLS | — |
| **Android Language** | Kotlin | 1.9.21 |
| **Android UI** | Jetpack Compose + Material 3 | Compose BOM 2024.02.00 |
| **Android Min SDK** | API 26 (Android 8.0) | — |
| **Android Target SDK** | API 34 | — |
| **Android Networking** | Retrofit 2.9 + OkHttp 4.12 | — |
| **Android Local DB** | Room | 2.6.1 |
| **Android DI** | Koin | 3.5.3 |
| **Android Preferences** | DataStore Preferences | 1.0.0 |
| **Android Background** | WorkManager | 2.9.0 |
| **Android Logging** | Timber | 5.0.1 |
| **Android Serialization** | kotlinx-serialization-json | 1.6.2 |
| **Build System** | Gradle | 8.5, AGP 8.2.0 |

---

## 16. Backend Architecture

### Entry Point (`main.py`)

The FastAPI application:
- Creates the app with lifespan handler (initializes Redis, creates DB tables)
- Registers 7 routers: `push`, `devices`, `keys`, `auth`, `history`, `rules`, `webhook`
- Adds CORS middleware
- Health check at `GET /health` and root at `GET /`
- Runs via: `uvicorn main:app --host 0.0.0.0 --port 8080 --workers 2`

### Layered Architecture

```
Nginx (TLS termination)
    ↓
Uvicorn (FastAPI)
    ↓
API Routes (api/*.py)
    ↓
Services (services/*.py)
    ↓
Database (PostgreSQL) + Cache (Redis)
```

### Configuration (`core/config.py`)

Uses Pydantic `BaseSettings` with automatic `.env` file loading:
- Validates `JWT_SECRET` is not the default value
- Defines rate limits
- Supports inline Firebase JSON or file path
- CORS origins configurable

### Security (`core/security.py`)

- **Password hashing**: bcrypt via passlib
- **JWT tokens**: HS256 algorithm, 7-day default expiry
- **API key generation**: `tk_` prefix + 32 random bytes, SHA-256 hashed for storage
- **API key verification**: Hashes incoming key, looks up in DB, rejects JWT tokens in API-key-only endpoints
- **Two auth modes**: JWT Bearer (user-facing) and API keys (programmatic)

---

## 17. Android App Architecture

### Package Structure

```
com.dusy4.pingbox/
├── TriggerApp.kt              ← Application class (notification channel setup)
├── MainActivity.kt            ← Compose ComponentActivity entry point
├── RouterActivity.kt          ← Transparent activity for intent routing + error handling
├── BootReceiver.kt            ← Re-registers FCM token on device boot
├── data/
│   ├── local/TriggerDatabase.kt   ← Room DB with fallbackToDestructiveMigration
│   ├── preferences/AppPreferences.kt ← DataStore for settings persistence
│   └── remote/TriggerApiService.kt ← Retrofit API interface + DTOs
├── di/AppModule.kt            ← Koin DI modules (network, preferences, ViewModels)
├── push/
│   ├── TriggerMessagingService.kt ← FirebaseMessagingService + RuleEngine
│   ├── DeviceRegistrationWorker.kt ← WorkManager device registration
│   └── UnifiedPushReceiver.kt ← UnifiedPush broadcast receiver
├── router/IntentRouter.kt     ← Intent building for all 5 target types
├── ui/
│   ├── screens/
│   │   ├── SettingsScreen.kt  ← Settings + Auth dialog + API key management
│   │   ├── RulesScreen.kt     ← Rules management (CRUD)
│   │   ├── HistoryScreen.kt   ← Notification history viewer
│   │   └── DebugScreen.kt     ← Debug endpoint testing
│   ├── viewmodel/
│   │   ├── SettingsViewModel.kt
│   │   ├── RulesViewModel.kt
│   │   └── HistoryViewModel.kt
│   ├── navigation/AppNavigation.kt ← Compose navigation + bottom bar
│   └── theme/                 ← Material 3 dark theme (Color, Type, Theme)
└── util/
    ├── ClipboardUtil.kt       ← Clipboard copy helper
    └── ScreenScale.kt         ← Adaptive screen scaling
```

### Key Components

#### IntentRouter (`router/IntentRouter.kt`)

Routes notification taps to the appropriate target:
- **url**: Opens browser with `ACTION_VIEW`
- **deeplink**: Same as url, but signals no browser fallback
- **package**: Gets launch intent via `getLaunchIntentForPackage()`
- **component**: Creates intent with explicit `ComponentName`
- **intent_uri**: Parses and creates from URI
- **none**: Returns to app (RouterActivity handles)

#### TriggerMessagingService (`push/TriggerMessagingService.kt`)

Firebase Cloud Messaging service:
1. Receives data-only messages
2. Loads enabled rules from local Room DB
3. RuleEngine resolves target based on notification tag
4. Creates PendingIntent with resolved target
5. Shows notification via NotificationManagerCompat
6. Saves to local history in Room DB

#### RuleEngine (`push/TriggerMessagingService.kt`)

Matches notification tags to rules:
1. Filters enabled rules
2. Sorts by priority (ascending, lower = higher priority)
3. Iterates finding first tag match (exact or wildcard `*`)
4. First match wins → returns rule's target
5. No match → returns payload's original target

#### DeviceRegistrationWorker (`push/DeviceRegistrationWorker.kt`)

WorkManager task that:
1. Gets FCM token (from input or fetches fresh)
2. Registers device with backend using API key auth
3. Stores device ID locally

---

## 18. Database Models

### PostgreSQL Schema (Backend)

All models use SQLAlchemy with PostgreSQL UUID primary keys and async sessions.

#### User (`models/user.py`)

| Column | Type | Notes |
|--------|------|-------|
| `id` | UUID | PK, auto-generated |
| `email` | Text | Unique, nullable |
| `password_hash` | Text | Nullable |
| `created_at` | DateTime | Auto-set on creation |

#### APIKey (`models/api_key.py`)

| Column | Type | Notes |
|--------|------|-------|
| `id` | UUID | PK |
| `user_id` | UUID | FK → users.id (CASCADE delete) |
| `device_id` | UUID | FK → devices.id (CASCADE delete), nullable |
| `key_hash` | Text | Unique, SHA-256 of raw key |
| `label` | Text | Nullable, user-facing name |
| `created_at` | DateTime | Auto-set |
| `revoked_at` | DateTime | Nullable; if set, key is invalid |

#### Device (`models/api_key.py`)

| Column | Type | Notes |
|--------|------|-------|
| `id` | UUID | PK |
| `user_id` | UUID | FK → users.id (CASCADE delete) |
| `fcm_token` | Text | Firebase Cloud Messaging token |
| `device_name` | Text | Nullable |
| `platform` | String(20) | Default: "android" |
| `created_at` | DateTime | Auto-set |
| `last_seen` | DateTime | Nullable |

#### NotificationLog (`models/notification.py`)

| Column | Type | Notes |
|--------|------|-------|
| `id` | UUID | PK |
| `user_id` | UUID | FK → users.id |
| `api_key_id` | UUID | FK → api_keys.id |
| `title` | Text | Required |
| `body` | Text | Nullable |
| `tag` | Text | Nullable |
| `target_type` | Text | Nullable |
| `target_value` | Text | Nullable |
| `status` | String(20) | Default: "sent" |
| `created_at` | DateTime | Auto-set |

#### Rule (`models/notification.py`)

| Column | Type | Notes |
|--------|------|-------|
| `id` | UUID | PK |
| `user_id` | UUID | FK → users.id |
| `name` | Text | Required |
| `match_tag` | Text | Nullable, supports wildcard `*` |
| `target_type` | Text | Required |
| `target_value` | Text | Required |
| `priority` | Integer | Default: 0 |
| `enabled` | Boolean | Default: true |
| `updated_at` | DateTime | Auto-updated |

#### Webhook (`models/notification.py`)

| Column | Type | Notes |
|--------|------|-------|
| `id` | UUID | PK |
| `user_id` | UUID | FK → users.id |
| `label` | Text | Nullable |
| `mapping` | JSON | JSONPath mappings for title/body/tag/target |
| `device_ids` | ARRAY(UUID) | Nullable array of device UUIDs |
| `secret` | Text | Nullable (for HMAC signature verification) |
| `created_at` | DateTime | Auto-set |

### Room Schema (Android)

#### NotificationEntity

| Column | Type | Notes |
|--------|------|-------|
| `id` | String | Unique ID |
| `title` | Text | Notification title |
| `body` | Text | Notification body |
| `icon` | Text | Icon/emoji |
| `tag` | Text | Tag for rule matching |
| `targetType` | Text | Resolved target type |
| `targetValue` | Text | Resolved target value |
| `receivedAt` | Long | Timestamp (millis) |

#### RuleEntity

| Column | Type | Notes |
|--------|------|-------|
| `id` | String | Rule ID |
| `name` | Text | Rule name |
| `matchTag` | Text | Tag pattern (wildcard support) |
| `targetType` | Text | Target type |
| `targetValue` | Text | Target value |
| `priority` | Int | Lower = higher priority |
| `enabled` | Boolean | Whether rule is active |
| `createdAt` | Long | Timestamp |
| `updatedAt` | Long | Timestamp |

---

## 19. API Reference — All Endpoints

### Base URL

```
https://your-server/
```

For local development: `http://localhost:8080/`

### Authentication

Two authentication modes:

1. **JWT Bearer Token** — Used for user-facing endpoints (auth, devices list, history, rules)
   ```
   Authorization: Bearer <jwt_token>
   ```

2. **API Key** — Used for programmatic access (push, device registration)
   ```
   Authorization: Bearer tk_XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
   ```

### Health & Root

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/` | No | API info and version |
| GET | `/health` | No | Health check |

### Authentication (`/v1/auth`)

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/v1/auth/register` | No | Register new user (email/password) |
| POST | `/v1/auth/login` | No | Login user (email/password) |
| POST | `/v1/auth/token` | No | OAuth2-compatible token endpoint |
| GET | `/v1/auth/me` | JWT | Get current user profile |

**Register Request:**
```json
{"email": "user@example.com", "password": "securepassword"}
```

**Register/Login Response:**
```json
{
  "access_token": "eyJhbGci...",
  "token_type": "bearer",
  "user": {
    "id": "uuid-here",
    "email": "user@example.com"
  }
}
```

### API Keys (`/v1/keys`)

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/v1/keys` | JWT | Create new API key (returned once, raw) |
| GET | `/v1/keys` | JWT | List all active API keys |
| DELETE | `/v1/keys/{key_id}` | JWT | Revoke an API key |

**Create Key Response:**
```json
{
  "id": "uuid",
  "key": "tk_XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX",
  "label": null,
  "created_at": "2026-04-04T12:00:00Z"
}
```

**Note**: The raw key (`tk_...`) is returned **only once** at creation. It cannot be retrieved later.

### Devices (`/v1/devices`)

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/v1/devices` | API Key | Register a device (FCM token) |
| GET | `/v1/devices` | JWT | List all registered devices |
| DELETE | `/v1/devices/{device_id}` | JWT or API Key | Remove a device |

**Register Device Request:**
```json
{
  "fcm_token": "fcm_token_string",
  "device_name": "My Phone",
  "platform": "android"
}
```

### Push Notifications (`/v1/push`)

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/v1/push` | API Key | Send a notification |
| POST | `/v1/push/batch` | API Key | Send batch (up to 50) |

**Push Request:**
```json
{
  "title": "Build Failed",
  "body": "main branch — commit a3f21c",
  "tag": "ci_fail",
  "priority": "high",
  "target": {
    "type": "url",
    "value": "https://github.com/user/repo/actions/runs/123"
  },
  "devices": ["all"]
}
```

**Push Response:**
```json
{
  "id": "ntf_abc123def456",
  "status": "sent",
  "devices_reached": 1
}
```

### Rules (`/v1/rules`)

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/v1/rules` | JWT | List all rules |
| POST | `/v1/rules` | JWT | Create a rule |
| PUT | `/v1/rules/{rule_id}` | JWT | Update a rule |
| DELETE | `/v1/rules/{rule_id}` | JWT | Delete a rule |

**Create Rule Request:**
```json
{
  "name": "Open GitHub on CI alerts",
  "match_tag": "ci_*",
  "target_type": "package",
  "target_value": "com.github.android",
  "priority": 0,
  "enabled": true
}
```

### History (`/v1/history`)

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/v1/history` | JWT | Get notification history (paginated) |

**Query Parameters:**
- `page` (default: 1) — Page number
- `limit` (default: 50) — Items per page

**History Response:**
```json
{
  "notifications": [...],
  "total": 100,
  "page": 1,
  "limit": 50
}
```

### Webhooks (`/v1/webhooks`)

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/v1/webhooks` | JWT | List webhook endpoints |
| POST | `/v1/webhooks` | JWT | Create webhook endpoint |
| DELETE | `/v1/webhooks/{webhook_id}` | JWT | Delete webhook |
| POST | `/v1/webhooks/{webhook_id}/receive` | No | Receive webhook (secured by unguessable UUID) |

**Create Webhook Request:**
```json
{
  "label": "Grafana Alerts",
  "mapping": {
    "title": "$.commonLabels.alertname",
    "body": "$.commonAnnotations.summary",
    "tag": "$.status",
    "target_type": "$.target.type",
    "target_value": "$.target.value"
  },
  "device_ids": []
}
```

### Error Responses

| Status Code | Meaning |
|-------------|---------|
| 400 | Bad request (e.g., email already registered) |
| 401 | Unauthorized (invalid/missing credentials) |
| 403 | Forbidden (e.g., API key limit reached) |
| 404 | Not found |
| 422 | Validation error (Pydantic) |
| 429 | Rate limit exceeded |
| 500 | Internal server error |

---

## 20. Services

### FCM Service (`services/fcm.py`)

- Sends push notifications via Firebase Admin SDK
- Requires `FIREBASE_CREDENTIALS_JSON` environment variable (full service account JSON)
- Uses data-only messages (no `notification` key in payload)
- Sets Android priority to `high` for immediate delivery
- Falls back gracefully if FCM is unavailable
- Logs `devices_reached` count in response

### Rate Limiting (`services/rate_limit.py`)

- Redis-based sliding window algorithm
- Per-API-key rate limiting
- Default limit: 60 requests per minute (configurable via env vars)
- Returns `HTTP 429 Too Many Requests` with `Retry-After` header when exceeded

### Webhook Service (`services/webhook.py`)

- HMAC signature verification for incoming webhooks
- JSONPath-based field extraction from arbitrary JSON payloads
- Maps external service payloads to PingBox notification format
- Supports optional secret header for verification

---

## 21. Android Components

### IntentRouter

The most important piece of code in the app. Handles all five target types:

```kotlin
object IntentRouter {
    fun buildIntent(context: Context, target: ResolvedTarget): Intent {
        return when (target.type) {
            "url" -> Intent(Intent.ACTION_VIEW, Uri.parse(target.value))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            "deeplink" -> Intent(Intent.ACTION_VIEW, Uri.parse(target.value))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            "package" -> context.packageManager
                .getLaunchIntentForPackage(target.value)
                ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                ?: createFallbackIntent(context, "App not found")

            "component" -> {
                val parts = target.value.split("/")
                Intent().apply {
                    component = ComponentName(parts[0], parts[1])
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }

            "intent_uri" -> Intent.parseUri(target.value, Intent.URI_INTENT_SCHEME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            else -> Intent(context, RouterActivity::class.java)
                .putExtra("notification_id", target.notificationId)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
```

### RouterActivity

A transparent activity that handles intent execution and error catching:
- No UI (`Theme.TriggerApp.Transparent`)
- Receives the target intent via `putExtra("target_intent", ...)`
- Attempts to `startActivity()` with the target intent
- Catches `ActivityNotFoundException` → shows Toast
- Catches `SecurityException` → shows Toast (activity not exported)
- Immediately calls `finish()` — invisible to user

### TriggerMessagingService

Firebase Cloud Messaging service:
1. Parses incoming data payload into `NotificationPayload`
2. Loads enabled rules from local Room DB
3. Runs `RuleEngine.resolve()` to determine final target
4. Builds PendingIntent with resolved target
5. Shows notification via `NotificationManagerCompat`
6. Saves to local Room history
7. On token refresh (`onNewToken`), enqueues `DeviceRegistrationWorker`

### RuleEngine

```kotlin
object RuleEngine {
    fun resolve(payload: NotificationPayload, rules: List<LocalRule>): ResolvedTarget {
        val enabledRules = rules.filter { it.enabled }.sortedBy { it.priority }
        
        for (rule in enabledRules) {
            if (matchesTag(payload.tag, rule.matchTag)) {
                return ResolvedTarget(
                    type = rule.targetType,
                    value = rule.targetValue,
                    notificationId = payload.id
                )
            }
        }
        
        // No rule matched → use payload's default target
        return ResolvedTarget(
            type = payload.targetType,
            value = payload.targetValue,
            notificationId = payload.id
        )
    }
    
    private fun matchesTag(tag: String, pattern: String?): Boolean {
        if (pattern.isNullOrEmpty()) return false
        return when {
            pattern.contains("*") -> tag.matches(pattern.replace("*", ".*").toRegex())
            else -> tag == pattern
        }
    }
}
```

### DeviceRegistrationWorker

Background WorkManager task:
- Gets FCM token from input data or fetches fresh from Firebase
- Reads API key from DataStore preferences
- Registers device with backend via `POST /v1/devices`
- Enqueued on: FCM token refresh, device boot, initial login

### BootReceiver

Broadcast receiver for `BOOT_COMPLETED`:
- Re-registers FCM token with backend
- Ensures push delivery survives device reboots

### UnifiedPushReceiver

Broadcast receiver for UnifiedPush distributor messages:
- Receives pushes from decentralized distributors (ntfy, Gotify-UP, etc.)
- Uses the same processing pipeline as FCM (RuleEngine → IntentRouter → Notification)
- Enables de-Googled device support

---

## 22. Data Flow Diagrams

### Receiving a Notification

```
FCM ──▶ TriggerMessagingService.onMessageReceived()
    │
    ├──▶ RuleEngine.resolve(payload, localRules)
    │       │
    │       └──▶ Check tag matching (exact + wildcard)
    │
    ├──▶ IntentRouter.buildPendingIntentTarget()
    │       │
    │       └──▶ Create PendingIntent with target Intent
    │
    ├──▶ NotificationManager.notify()
    │       │
    │       └──▶ User sees notification in system tray
    │
    └──▶ saveToLocalHistory()
            │
            └──▶ Room DB (NotificationEntity)
```

### User Taps Notification

```
User Tap ──▶ PendingIntent ──▶ RouterActivity
    │
    ├──▶ IntentRouter.buildIntent()
    │       │
    │       └──▶ Create underlying Intent for target
    │
    ├──▶ startActivity(underlyingIntent)
    │       │
    │       └──▶ Target app/URL opens
    │
    └──▶ finish()
            └──▶ RouterActivity closes (invisible)
```

### Login → API Key Generation Flow

```
User enters email/password
    │
    ▼
SettingsViewModel.login() / register()
    │
    ▼
POST /v1/auth/login or /v1/auth/register
    │
    ▼
Backend returns JWT access_token + user info
    │
    ▼
Token saved to DataStore + UI state updated immediately
    │
    ▼
SettingsScreen receives LoggedIn event
    │
    ▼
viewModel.generateApiKey() called automatically
    │
    ▼
POST /v1/keys (with Bearer token)
    │
    ▼
Backend creates API key, returns raw tk_... key
    │
    ▼
API key saved to DataStore → displayed in UI → copiable
```

---

## 23. Security

### Backend Security

1. **API Keys**: Stored as SHA-256 hashes, never in plaintext. Raw key shown once at creation.
2. **JWT Tokens**: HS256 algorithm with configurable expiration (default 7 days).
3. **Rate Limiting**: Per-key Redis sliding window.
4. **Input Validation**: Pydantic models validate all input (email format, string lengths, etc.).
5. **SQL Injection**: Prevented via SQLAlchemy ORM with parameterized queries.
6. **CORS**: Configurable allowed origins via environment variable.
7. **Password Hashing**: bcrypt via passlib.

### Android Security

1. **API Key Storage**: Stored in DataStore Preferences (consider EncryptedSharedPreferences for production).
2. **JWT Secret Validation**: Backend refuses to start with default JWT secret.
3. **ProGuard/R8**: Enabled for release builds.
4. **HTTPS Only**: All connections use TLS.

### Best Practices for Production

1. Use HTTPS for all connections (Nginx with Let's Encrypt)
2. Implement certificate pinning in Android app
3. Use EncryptedSharedPreferences for sensitive data
4. Set up proper Firebase security rules
5. Implement webhook signature verification
6. Use environment variables for all secrets
7. Set proper CORS origins (not `*`)
8. Generate strong random JWT secret (`python3 -c "import secrets; print(secrets.token_hex(32))"`)
9. Regularly rotate API keys
10. Monitor rate limit hits for abuse detection

---

## 24. Development Setup

### Prerequisites

- Python 3.12+
- Docker & Docker Compose
- Android Studio (Arctic Fox or later) / Gradle 8.5
- Firebase project with `google-services.json`
- JDK 17

### Backend Development

```bash
# 1. Clone and configure
git clone https://github.com/your-org/pingbox.git
cd pingbox
cp .env.example .env
# Edit .env with your secrets

# 2. Start all services (PostgreSQL, Redis, API, Nginx)
python bootstrap.py redeploy

# 3. View logs
python bootstrap.py logs --service api --follow

# 4. Test health
curl http://localhost:8080/health
# Expected: {"status": "healthy"}

# 5. Access API docs (Swagger UI)
# Open http://localhost:8080/docs in browser
```

### Android Development

```bash
# 1. Open in Android Studio
# File > Open > select android/ folder

# 2. Place google-services.json
# Copy your Firebase google-services.json to android/app/

# 3. Sync Gradle
# Android Studio should prompt automatically

# 4. Build debug APK
cd android
./gradlew assembleDebug

# 5. Install on device/emulator
./gradlew installDebug

# 6. Run tests
./gradlew test
```

### Firebase Setup

1. Create a Firebase project at https://console.firebase.google.com
2. Add an Android app with package name `com.dusy4.pingbox`
3. Download `google-services.json` and place in `android/app/`
4. Generate a service account key: Project Settings > Service Accounts > Generate New Private Key
5. Paste the JSON content into `FIREBASE_CREDENTIALS_JSON` in your `.env` file

---

## 25. Environment Variables Reference

All configuration is managed through a single `.env` file at the project root. Copy `.env.example` to `.env` and fill in your values.

### Backend Variables

| Variable | Required | Description | Example |
|----------|----------|-------------|---------|
| `POSTGRES_USER` | Yes | PostgreSQL username | `triggerapp` |
| `POSTGRES_PASSWORD` | Yes | PostgreSQL password | `secure-password-here` |
| `POSTGRES_DB` | Yes | PostgreSQL database name | `triggerapp` |
| `DATABASE_URL` | Yes | Full async PostgreSQL connection string | `postgresql+asyncpg://user:pass@db:5432/db` |
| `REDIS_PASSWORD` | Yes | Redis password | `secure-redis-pass` |
| `REDIS_URL` | Yes | Full Redis connection string with auth | `redis://:pass@redis:6379` |
| `FIREBASE_CREDENTIALS_JSON` | Yes | Full Firebase service account JSON (single line) | `{"type":"service_account",...}` |
| `JWT_SECRET` | Yes | JWT signing key (min 32 chars) | `$(python3 -c "import secrets; print(secrets.token_hex(32))")` |
| `CORS_ORIGINS` | Yes | Comma-separated allowed origins | `http://localhost:3000,https://your-domain.com` |

### Android Variables

| Variable | Required | Description |
|----------|----------|-------------|
| `ANDROID_SERVER_URL` | No | Base URL for development builds |
| `ANDROID_FIREBASE_API_KEY` | No | Firebase Web API key from google-services.json |
| `ANDROID_FIREBASE_PROJECT_ID` | No | Firebase project ID |
| `ANDROID_FIREBASE_APP_ID` | No | Firebase app ID |

### Generating Secure Values

```bash
# JWT Secret
python3 -c "import secrets; print(secrets.token_hex(32))"

# Database Password
python3 -c "import secrets; print(secrets.token_urlsafe(32))"

# Redis Password
python3 -c "import secrets; print(secrets.token_urlsafe(32))"
```

---

## 26. Container Management (bootstrap.py)

The `bootstrap.py` script at the project root manages Docker containers for the backend. It requires **no external dependencies** — only `docker-compose` CLI installed.

### Quick Reference

```bash
# Full redeploy (stop → rebuild → start) — for backend code updates
python bootstrap.py redeploy

# Same but force clean build (no cache)
python bootstrap.py redeploy --no-cache

# Individual operations
python bootstrap.py down              # Stop containers
python bootstrap.py build             # Rebuild images
python bootstrap.py up                # Start containers
python bootstrap.py build up          # Chain: build then up
python bootstrap.py restart           # Restart running containers
python bootstrap.py logs --follow     # Follow logs in real-time
python bootstrap.py status            # Show container status
```

### Target Specific Services

Available services: `api`, `nginx`, `db`, `redis`

```bash
# Rebuild only the API service
python bootstrap.py build --service api

# Restart only nginx
python bootstrap.py restart --service nginx

# View logs for database only
python bootstrap.py logs --service db --follow

# Start specific services only
python bootstrap.py up --services api nginx
```

### Dangerous Operations

⚠️ **WARNING: These will delete data!**

```bash
# Stop and remove volumes (DATABASE DATA WILL BE LOST!)
python bootstrap.py down --remove-volumes

# Stop, remove volumes AND images
python bootstrap.py down --remove-volumes --remove-images
```

### All CLI Options

| Flag | Short | Description |
|------|-------|-------------|
| `--service <name>` | `-s` | Target specific service |
| `--services <names>` | — | Multiple services for `up` |
| `--no-cache` | — | Build without cache |
| `--remove-volumes` | `-v` | Remove volumes on `down` (DANGER) |
| `--remove-images` | `-i` | Remove images on `down` |
| `--detach` | `-d` | Run containers in background (default) |
| `--no-detach` | — | Run containers in foreground |
| `--follow` | `-f` | Follow log output |
| `--tail <N>` | `-t` | Show last N log lines |

### Troubleshooting Containers

```bash
# View last 100 lines of logs
python bootstrap.py logs --tail 100

# Check why a container exited
python bootstrap.py logs --service api

# Full restart with fresh build
python bootstrap.py down && python bootstrap.py redeploy --no-cache

# Check container status
python bootstrap.py status
```

### Common Issues

| Issue | Solution |
|-------|----------|
| "Docker daemon is not running" | `sudo systemctl start docker` |
| "Permission denied" | Use `sudo python bootstrap.py ...` |
| "docker-compose not found" | Install docker-compose |
| Database not ready | Wait for healthcheck (5s interval, 5 retries) |
| Port conflict | Change port mapping in docker-compose.yml |

---

## 27. Deployment

### Production Deployment (Docker Compose)

The production setup uses Docker Compose with 4 services:

```yaml
services:
  nginx:          # Reverse proxy + TLS termination (Let's Encrypt)
  api:            # FastAPI application (2 uvicorn workers)
  db:             # PostgreSQL 16 with healthcheck
  redis:          # Redis 7 with password auth
```

### Build Image

```bash
cd backend
docker build -t triggerapp-api:latest .
```

### Nginx Configuration

The included `nginx.conf`:
- Serves your domain
- HTTP → HTTPS redirect
- TLS via Let's Encrypt (`/etc/letsencrypt`)
- Proxies to `http://api:8080`

### Android Release Build

```bash
cd android
./gradlew assembleRelease
```

Sign the APK using Android Studio's Generate Signed APK wizard or configure signing in `build.gradle.kts`.

---

## 28. Testing

### Backend Endpoint Testing

All endpoints have been tested and verified. Here's the test status as of 2026-04-04:

| Endpoint | Method | Status | Notes |
|----------|--------|--------|-------|
| `/` | GET | ✅ Working | Returns API info |
| `/health` | GET | ✅ Working | Returns `{"status":"healthy"}` |
| `/v1/auth/register` | POST | ✅ Working | Returns JWT + user |
| `/v1/auth/login` | POST | ✅ Working | Returns JWT + user |
| `/v1/auth/me` | GET | ✅ Working | Returns user profile |
| `/v1/keys` | POST | ✅ Working | Creates API key |
| `/v1/keys` | GET | ✅ Working | Lists keys |
| `/v1/keys/{id}` | DELETE | ✅ Working | Revokes key |
| `/v1/devices` | POST | ✅ Working | Registers device |
| `/v1/devices` | GET | ✅ Working | Lists devices |
| `/v1/devices/{id}` | DELETE | ✅ Fixed | Was 500, now supports API key auth |
| `/v1/push` | POST | ✅ Working | Sends push (status: "failed" with test FCM token — expected) |
| `/v1/rules` | POST | ✅ Working | Creates rule |
| `/v1/rules` | GET | ✅ Working | Lists rules |
| `/v1/rules/{id}` | PUT | ✅ Working | Updates rule |
| `/v1/rules/{id}` | DELETE | ✅ Working | Deletes rule |
| `/v1/history` | GET | ✅ Working | Returns paginated history |

### Android Unit Testing

```bash
# Run all unit tests
cd android && ./gradlew test

# Run specific test class
./gradlew test --tests "com.dusy4.pingbox.push.RuleEngineTest"

# Run with coverage
./gradlew testDebugUnitTestCoverage
```

### Test Categories

- **RuleEngine Tests**: Exact tag matching, wildcard patterns, priority ordering, disabled rules
- **IntentRouter Tests**: URL/deeplink/package/component/intent_uri intent creation, fallback handling
- **Auth API Tests**: Login/register success/failure cases
- **Logger Tests**: Log buffer, filtering, export functionality

---

## 29. Edge Cases & Error Handling

| Scenario | Expected Behavior | Status |
|----------|------------------|--------|
| Target app not installed | `ActivityNotFoundException` caught in RouterActivity → Toast "App not installed" | ✅ Implemented |
| Target activity has `android:exported="false"` | `SecurityException` caught → Toast "Activity not accessible" → fallback to main activity | ✅ Implemented |
| Invalid URL format | Rejected at backend (HTTP 422) before FCM dispatch | ✅ Implemented |
| FCM token expired / invalid | FCM returns `UNREGISTERED` → backend marks as failed | ✅ Implemented |
| Device offline when push sent | FCM queues message for up to 28 days (default TTL) | ✅ Works |
| Payload exceeds 4000 bytes | Backend rejects before reaching FCM | ✅ Implemented |
| Rate limit exceeded | HTTP 429 + `Retry-After` header | ✅ Implemented |
| Backend is down | FCM delivery already dispatched continues working. New pushes get HTTP 500. | ✅ Works |
| POST_NOTIFICATIONS not granted | Android 13+ requires runtime permission. Pipeline runs but `notify()` silently fails. | ✅ Implemented |
| Multiple devices receive same notification | Each device independently processes — no deduplication needed | ✅ Works |
| Rule engine matches wrong rule | Rules are priority-ordered. First match wins. | ✅ Implemented |
| API key from previous session | App detects orphaned key and shows warning on login | ✅ Implemented |
| API key not displayed after login | **Fixed**: Race condition resolved — token set immediately in UI state | ✅ Fixed |
| Package not visible on Android 11+ | **Fixed**: Added `<queries>` element + `QUERY_ALL_PACKAGES` permission | ✅ Fixed |

---

## 30. Android Permissions

```xml
<!-- Required: internet for FCM + backend API -->
<uses-permission android:name="android.permission.INTERNET" />

<!-- Required Android 13+: show notifications (runtime request) -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<!-- Required: re-register FCM token after device reboot -->
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />

<!-- Required: see all installed packages for package-type targets -->
<uses-permission android:name="android.permission.QUERY_ALL_PACKAGES" />

<!-- Package visibility queries for Android 11+ -->
<queries>
    <intent>
        <action android:name="android.intent.action.VIEW" />
        <data android:scheme="https" />
    </intent>
    <intent>
        <action android:name="android.intent.action.VIEW" />
        <data android:scheme="http" />
    </intent>
    <intent>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent>
</queries>
```

**Root / Shizuku: NEVER required.** PingBox only targets:
- Exported activities (no permission needed)
- Deep links and URI schemes (no permission needed)
- Launch intents for installed apps (no permission needed)

Non-exported activities are gracefully handled as errors, with a fallback to the app's main screen.

---

## 31. Debug & Logging System

### Architecture

The app uses **Timber** for logging with structured tags:

```kotlin
Timber.tag("TriggerMessaging").d("Message received with ${data.size} data fields")
Timber.tag("RuleEngine").i("Rule matched: ${rule.name} (${rule.matchTag})")
Timber.tag("TriggerMessaging").e(e, "Error processing notification")
```

### Debug Screen

Accessible from Settings → "Debug Menu" (bottom of screen):
- Test endpoint connectivity
- View current configuration
- Check auth status
- Test API key validity

### Log Tags

| Tag | Component | Description |
|-----|-----------|-------------|
| `TriggerMessaging` | FCM Service | Message receipt and processing |
| `RuleEngine` | Rule Engine | Rule matching resolution |
| `DeviceRegistration` | WorkManager | Device registration attempts |
| `SettingsViewModel` | Settings | Auth and API key operations |

### Backend Debug Endpoints

```
GET /health           # Check server health
GET /                 # API info and version
GET /docs             # Swagger UI (interactive API docs)
GET /redoc            # ReDoc API documentation
```

---

## 32. Unit Testing

### Test Structure

```
android/app/src/test/java/com/dusy4/pingbox/
├── data/remote/AuthApiTest.kt      # Auth API tests
├── push/RuleEngineTest.kt          # Rule matching logic tests
├── router/IntentRouterTest.kt      # Intent building tests
└── util/LoggerTest.kt              # Logger functionality tests
```

### Running Tests

```bash
# Run all unit tests
./gradlew test

# Run specific test class
./gradlew test --tests "com.dusy4.pingbox.push.RuleEngineTest"

# Run with coverage
./gradlew testDebugUnitTestCoverage
```

### Test Dependencies

```kotlin
testImplementation "junit:junit:4.13.2"
testImplementation "org.robolectric:robolectric:4.11.1"
testImplementation "org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3"
testImplementation "io.mockk:mockk:1.13.8"
testImplementation "com.squareup.okhttp3:mockwebserver:4.12.0"
```

---

## 33. Future Enhancements

### Planned
- [ ] Push notification delivery confirmation
- [ ] Rich notifications with actions
- [ ] Notification scheduling
- [ ] Notification templates
- [ ] End-to-end encryption
- [ ] Multi-user support with team management
- [ ] Analytics dashboard
- [ ] Web interface for management
- [ ] Email/SMS notifications as output
- [ ] iOS app (APNs transport)
- [ ] Official SDK libraries: Python, Node.js, Go
- [ ] CLI tool (`trigger send --title "..." --url "..."`)
- [ ] F-Droid release (UnifiedPush-only build, no FCM)
- [ ] Scheduled notifications
- [ ] Home screen widget (recent notifications)
- [ ] Notification grouping and channels per tag

### In Progress / Partially Implemented
- [x] UnifiedPush support (receiver implemented, needs full integration)
- [x] Webhook receiver with JSONPath mapping

---

## 34. Development Roadmap

### Phase 1 — MVP (Weeks 1–6) ✅ COMPLETE

**Goal**: End-to-end working system. `curl → notification → tap → app opens`.

**Completed:**
- [x] FastAPI backend with all core endpoints
- [x] `POST /v1/push` with API key auth
- [x] Device registration endpoint
- [x] FCM integration (data-only messages)
- [x] Redis rate limiting
- [x] Docker Compose deployment
- [x] FirebaseMessagingService with payload parsing
- [x] IntentRouter for all 5 target types
- [x] RouterActivity (transparent error handler)
- [x] Settings screen with auth and API key management
- [x] Room DB for local history
- [x] Local rules with tag matching
- [x] FCM token re-registration worker
- [x] Boot receiver for token re-registration

### Phase 2 — Cloud Features & Integrations

- [ ] Google/GitHub OAuth2 login
- [ ] Cloud notification history API
- [ ] Cloud rules CRUD API
- [ ] Webhook receiver + JSONPath mapping engine
- [ ] Web dashboard
- [ ] WebSocket endpoint
- [ ] Cloud sync module
- [ ] Multi-device support

### Phase 3 — Power Features

- [ ] Pre-built webhook templates
- [ ] MQTT broker integration
- [ ] Advanced analytics
- [ ] NotificationListenerService integration
- [ ] WebSocket fallback channel
- [ ] Notification grouping
- [ ] Home screen widget

### Phase 4 — Growth

- [ ] iOS app
- [ ] Official SDK libraries (Python, Node.js, Go)
- [ ] CLI tool
- [ ] F-Droid release
- [ ] Scheduled notifications

---

## 35. Licensing

### ntfy and AGPL

ntfy is licensed under **AGPLv3**. PingBox does **NOT** fork or embed ntfy code. PingBox is written from scratch. ntfy can be used as an **optional external transport layer** (a separately running service that PingBox talks to via HTTP), which does not trigger AGPL requirements on PingBox's own codebase.

PingBox is licensed under the MIT License — see [LICENSE](LICENSE) for details.

---

## Appendix A: Common Deep Link URI Schemes

| App | URI Scheme | Example |
|-----|-----------|---------|
| Telegram | `tg://` | `tg://resolve?domain=username` |
| Instagram | `instagram://` | `instagram://user?username=xyz` |
| YouTube | `youtube://` | `youtube://watch?v=VIDEO_ID` |
| Spotify | `spotify:` | `spotify:track:TRACK_ID` |
| GitHub (Android) | `https://github.com` | Claimed via App Links |
| Chrome | `googlechrome://` | `googlechrome://navigate?url=https://...` |
| Firefox | `firefox://` | `firefox://open-url?url=https://...` |
| Discord | `discord://` | `discord://channels/SERVER_ID/CHANNEL_ID` |
| Slack | `slack://` | `slack://channel?id=...&team=...` |

---

## Appendix B: Webhook Integration Templates

### Grafana Alert

```json
{
  "mapping": {
    "title": "$.commonLabels.alertname",
    "body": "$.commonAnnotations.summary",
    "tag": "$.status",
    "target_type": "url",
    "target_value": "$.externalURL"
  }
}
```

### GitHub Actions (workflow_run)

```json
{
  "mapping": {
    "title": "$.workflow.name",
    "body": "$.workflow_run.conclusion — ${{ github.ref }}",
    "tag": "ci_${workflow_run.conclusion}",
    "target_type": "url",
    "target_value": "$.workflow_run.html_url"
  }
}
```

### Stripe (payment.succeeded)

```json
{
  "mapping": {
    "title": "Payment Received",
    "body": "$.data.object.amount — $.data.object.currency",
    "tag": "payment",
    "target_type": "url",
    "target_value": "https://dashboard.stripe.com/payments"
  }
}
```

### Uptime Kuma

```json
{
  "mapping": {
    "title": "$.monitorName",
    "body": "$.msg",
    "tag": "monitor_${status}",
    "target_type": "url",
    "target_value": "$.URL"
  }
}
```

### Home Assistant

```json
{
  "mapping": {
    "title": "$.title",
    "body": "$.message",
    "tag": "$.tag",
    "target_type": "$.data.target_type",
    "target_value": "$.data.target_value"
  }
}
```

### Prometheus Alertmanager

```json
{
  "mapping": {
    "title": "$.alerts.0.labels.alertname",
    "body": "$.alerts.0.annotations.description",
    "tag": "prometheus_${alerts.0.status}",
    "target_type": "url",
    "target_value": "$.externalURL"
  }
}
```

### GitLab CI/CD Pipeline

```json
{
  "mapping": {
    "title": "$.pipeline.name",
    "body": "$.build_name — $.build_status",
    "tag": "gitlab_${build_status}",
    "target_type": "url",
    "target_value": "$.pipeline.url"
  }
}
```

---

## Appendix C: Local Rule Example

```json
{
  "id": "rule_001",
  "name": "Open GitHub app on CI alerts",
  "matchTag": "ci_*",
  "targetType": "package",
  "targetValue": "com.github.android",
  "priority": 1,
  "enabled": true
}
```

This means: any notification with a tag starting with `ci_` (e.g., `ci_fail`, `ci_pass`) → ignore whatever target the push payload specified → instead, open the GitHub Android app.

---

## Appendix D: bootstrap.py Full Reference

### Overview

The `bootstrap.py` script is a Docker container management tool for the PingBox backend. It uses only Python standard library (`subprocess`) — no pip install needed. Just needs `docker-compose` CLI installed.

### Architecture

```
bootstrap.py
    │
    ├── containers_down()     # Stop containers (optional: remove volumes/images)
    ├── containers_build()    # Build images (docker build + docker-compose build)
    ├── containers_up()       # Start containers (detached by default)
    ├── containers_restart()  # Restart running containers
    ├── containers_logs()     # View/follow logs
    ├── containers_status()   # Show docker-compose ps output
    └── full_redeploy()       # Orchestrates: down → build → up
```

### Usage Examples

```bash
# === MOST COMMON ===
# Full redeploy (after code changes)
python bootstrap.py redeploy

# Clean rebuild (no cache)
python bootstrap.py redeploy --no-cache

# === INDIVIDUAL OPERATIONS ===
python bootstrap.py down              # Stop all containers
python bootstrap.py build             # Rebuild all images
python bootstrap.py up                # Start all containers
python bootstrap.py build up          # Chain: build then start
python bootstrap.py restart           # Restart all containers
python bootstrap.py logs --follow     # Follow all logs
python bootstrap.py status            # Show container status

# === SERVICE-SPECIFIC ===
python bootstrap.py build --service api
python bootstrap.py restart --service nginx
python bootstrap.py logs --service db --follow
python bootstrap.py logs --service api --tail 100
python bootstrap.py up --services api nginx

# === DANGEROUS ===
python bootstrap.py down --remove-volumes           # Removes DB data!
python bootstrap.py down --remove-volumes --remove-images  # Nuclear option
```

### Error Handling

The script provides helpful error messages for common issues:
- "Docker daemon is not running" → suggests `sudo systemctl start docker`
- "Permission denied" → suggests using `sudo`
- "docker-compose not found" → suggests installing docker-compose

### Exit Codes

| Code | Meaning |
|------|---------|
| 0 | Success |
| 1 | Operation failed |
| 130 | Interrupted by user (Ctrl+C) |

---

*PingBox (TriggerApp) — Universal Notification Router for Android*
*Documentation last updated: 2026-04-04*
