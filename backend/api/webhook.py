from datetime import datetime, timezone
from typing import Annotated
import uuid
import json
import logging

from fastapi import APIRouter, Depends, HTTPException, status, Request
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from pydantic import BaseModel, Field
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from jsonpath_ng import parse

from core.database import get_db
from core.security import get_current_user, verify_api_key
from models.api_key import Device
from models.notification import NotificationLog, Webhook as WebhookModel
from services.fcm import send_fcm_message

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/v1/webhooks", tags=["webhooks"])


class WebhookMapping(BaseModel):
    title: str = Field(..., description="JSONPath to title field")
    body: str | None = Field(default=None, description="JSONPath to body field")
    tag: str | None = Field(default=None, description="JSONPath to tag field")
    target_type: str = Field(default="url", description="Target type override")
    target_value: str | None = Field(default=None, description="JSONPath to target value")


class WebhookConfig(BaseModel):
    mapping: WebhookMapping
    device_ids: list[str] | None = None


class WebhookPayload(BaseModel):
    title: str
    body: str | None = None
    tag: str | None = None
    target_type: str = "url"
    target_value: str = ""


def extract_jsonpath(data: dict, path: str) -> str | None:
    try:
        expr = parse(path)
        match = expr.find(data)
        if match:
            return str(match.value) if match.value is not None else None
    except Exception as e:
        logger.warning("JSONPath extraction failed for %s: %s", path, e)
    return None


@router.post("/{webhook_id}/receive")
async def receive_webhook(
    webhook_id: str,
    request: Request,
    db: AsyncSession = Depends(get_db),
):
    try:
        webhook_uuid = uuid.UUID(webhook_id)
    except ValueError:
        raise HTTPException(status_code=400, detail="Invalid webhook ID format")
    
    result = await db.execute(select(WebhookModel).where(WebhookModel.id == webhook_uuid))
    webhook_model = result.scalar_one_or_none()
    if not webhook_model:
        raise HTTPException(status_code=404, detail="Webhook not found")
    
    json_data = {}
    try:
        json_data = await request.json()
    except Exception:
        json_data = {}
        logger.warning("Webhook %s received invalid JSON", webhook_id)
    
    mapping = webhook_model.mapping
    title = extract_jsonpath(json_data, mapping.get("title", "")) or "Webhook Alert"
    body_val = extract_jsonpath(json_data, mapping.get("body", "")) if mapping.get("body") else None
    tag_val = extract_jsonpath(json_data, mapping.get("tag", "")) if mapping.get("tag") else None
    target_type = mapping.get("target_type", "url")
    target_value = extract_jsonpath(json_data, mapping.get("target_value", "")) if mapping.get("target_value") else ""
    
    device_ids = webhook_model.device_ids or []
    
    if device_ids:
        device_filter = select(Device).where(Device.id.in_(device_ids))
    else:
        device_filter = select(Device).where(Device.user_id == webhook_model.user_id)
    
    device_result = await db.execute(device_filter)
    devices = device_result.scalars().all()
    
    devices_reached = 0
    for device in devices:
        data_message = {
            "ntf_id": f"wh_{webhook_id[:8]}_{int(datetime.now(timezone.utc).timestamp())}",
            "title": title,
            "body": body_val or "",
            "tag": tag_val or "",
            "target_type": target_type,
            "target_value": target_value or "",
        }
        try:
            send_fcm_message(device.fcm_token, data_message)
            devices_reached += 1
            logger.info("Webhook push sent to device %s", device.id)
        except Exception as e:
            logger.error("Failed to send webhook push to device %s: %s", device.id, e)
    
    log_entry = NotificationLog(
        user_id=webhook_model.user_id,
        title=title,
        body=body_val,
        tag=tag_val,
        target_type=target_type,
        target_value=target_value or "",
        status="sent" if devices_reached > 0 else "no_devices",
    )
    db.add(log_entry)
    await db.commit()
    
    logger.info("Webhook %s received: title=%s, devices_reached=%d", webhook_id, title, devices_reached)
    
    return {"status": "received", "webhook_id": webhook_id, "devices_reached": devices_reached}


@router.post("")
async def create_webhook(
    config: WebhookConfig,
    credentials: Annotated[HTTPAuthorizationCredentials, Depends(HTTPBearer())],
    db: AsyncSession = Depends(get_db),
    label: str | None = None,
):
    user = await get_current_user(credentials, db)
    
    webhook_id = uuid.uuid4()
    new_webhook = WebhookModel(
        id=webhook_id,
        user_id=user.id,
        label=label or f"Webhook {str(webhook_id)[:8]}",
        mapping=config.mapping.model_dump(),
        device_ids=[uuid.UUID(d) for d in config.device_ids] if config.device_ids else None,
    )
    db.add(new_webhook)
    await db.commit()
    
    logger.info("Webhook %s created for user %s", webhook_id, user.id)
    return {"status": "created", "webhook_id": str(webhook_id)}


@router.get("")
async def list_webhooks(
    credentials: Annotated[HTTPAuthorizationCredentials, Depends(HTTPBearer())],
    db: AsyncSession = Depends(get_db),
):
    user = await get_current_user(credentials, db)
    result = await db.execute(select(WebhookModel).where(WebhookModel.user_id == user.id))
    webhooks = result.scalars().all()
    
    return {
        "webhooks": [
            {
                "id": str(w.id),
                "label": w.label,
                "device_ids": [str(d) for d in (w.device_ids or [])],
                "created_at": w.created_at.isoformat() if w.created_at else None,
            }
            for w in webhooks
        ]
    }


@router.delete("/{webhook_id}")
async def delete_webhook(
    webhook_id: str,
    credentials: Annotated[HTTPAuthorizationCredentials, Depends(HTTPBearer())],
    db: AsyncSession = Depends(get_db),
):
    user = await get_current_user(credentials, db)
    
    try:
        webhook_uuid = uuid.UUID(webhook_id)
    except ValueError:
        raise HTTPException(status_code=400, detail="Invalid webhook ID format")
    
    result = await db.execute(
        select(WebhookModel).where(WebhookModel.id == webhook_uuid, WebhookModel.user_id == user.id)
    )
    webhook = result.scalar_one_or_none()
    if not webhook:
        raise HTTPException(status_code=404, detail="Webhook not found")
    
    await db.delete(webhook)
    await db.commit()
    return {"status": "deleted"}


@router.put("/{webhook_id}")
async def update_webhook(
    webhook_id: str,
    config: WebhookConfig,
    credentials: Annotated[HTTPAuthorizationCredentials, Depends(HTTPBearer())],
    db: AsyncSession = Depends(get_db),
    label: str | None = None,
):
    user = await get_current_user(credentials, db)
    
    try:
        webhook_uuid = uuid.UUID(webhook_id)
    except ValueError:
        raise HTTPException(status_code=400, detail="Invalid webhook ID format")
    
    result = await db.execute(
        select(WebhookModel).where(WebhookModel.id == webhook_uuid, WebhookModel.user_id == user.id)
    )
    webhook = result.scalar_one_or_none()
    if not webhook:
        raise HTTPException(status_code=404, detail="Webhook not found")
    
    webhook.mapping = config.mapping.model_dump()
    webhook.device_ids = [uuid.UUID(d) for d in config.device_ids] if config.device_ids else None
    if label:
        webhook.label = label
    
    await db.commit()
    return {"status": "updated", "webhook_id": webhook_id}
