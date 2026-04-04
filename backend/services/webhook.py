import hmac
import hashlib
from typing import Optional

from core.config import get_settings

settings = get_settings()


def verify_webhook_signature(
    payload: bytes,
    signature: Optional[str],
    secret: Optional[str]
) -> bool:
    if not signature or not secret:
        return True
    
    if signature.startswith("sha256="):
        expected = signature[7:]
    else:
        expected = signature
    
    computed = hmac.new(
        secret.encode(),
        payload,
        hashlib.sha256
    ).hexdigest()
    
    return hmac.compare_digest(computed, expected)


def generate_webhook_secret() -> str:
    import secrets
    return secrets.token_urlsafe(32)
