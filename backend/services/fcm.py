import json
from typing import Any

import firebase_admin
from firebase_admin import credentials, messaging

from core.config import get_settings

settings = get_settings()

_firebase_app = None


def init_firebase():
    global _firebase_app
    if _firebase_app is not None:
        return
    
    if settings.firebase_credentials_json:
        cred = credentials.Certificate(json.loads(settings.firebase_credentials_json))
    elif settings.firebase_credentials_path and settings.firebase_credentials_path != "/run/secrets/firebase.json":
        cred = credentials.Certificate(settings.firebase_credentials_path)
    else:
        raise RuntimeError("Firebase credentials not configured")
    
    _firebase_app = firebase_admin.initialize_app(cred)


def send_fcm_message(token: str, data: dict[str, str]) -> str:
    if _firebase_app is None:
        init_firebase()
    
    message = messaging.Message(
        token=token,
        data=data,
        android=messaging.AndroidConfig(
            priority="high",
            notification=messaging.AndroidNotification(icon="ic_notification"),
        ),
    )
    
    response = messaging.send(message)
    return response


def send_fcm_multicast(tokens: list[str], data: dict[str, str]) -> dict[str, Any]:
    if _firebase_app is None:
        init_firebase()
    
    message = messaging.MulticastMessage(
        tokens=tokens,
        data=data,
        android=messaging.AndroidConfig(priority="high"),
    )
    
    response = messaging.send_multicast(message)
    return {
        "success_count": response.success_count,
        "failure_count": response.failure_count,
        "responses": [
            {"success": r.success, "message_id": r.message_id, "error": str(r.error) if r.error else None}
            for r in response.responses
        ],
    }
