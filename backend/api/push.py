from datetime import datetime, timezone
from typing import Annotated
from uuid import UUID
import asyncio
import logging

from fastapi import APIRouter, Depends, HTTPException, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from pydantic import BaseModel, Field
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
import uuid

logger = logging.getLogger(__name__)

from core.database import get_db
from core.security import verify_api_key, create_api_key
from models.user import User
from models.api_key import APIKey, Device
from models.notification import NotificationLog
from services.rate_limit import check_rate_limit

router = APIRouter(prefix="/v1", tags=["push"])
security = HTTPBearer()


class TargetPayload(BaseModel):
    type: str = Field(..., pattern="^(url|deeplink|package|component|intent_uri|none)$")
    value: str = Field(default="")


class PushPayload(BaseModel):
    title: str = Field(..., max_length=250)
    body: str = Field(default="", max_length=1000)
    icon: str | None = None
    tag: str | None = None
    priority: str = Field(default="high", pattern="^(high|normal)$")
    target: TargetPayload = Field(default_factory=lambda: TargetPayload(type="none"))
    devices: list[str] = Field(default_factory=lambda: ["all"])


class PushResponse(BaseModel):
    id: str
    status: str
    devices_reached: int


class BatchPushPayload(BaseModel):
    notifications: list[PushPayload] = Field(max_length=50)


class BatchPushResponse(BaseModel):
    results: list[PushResponse]


@router.post("/push", response_model=PushResponse)
async def send_push(
    payload: PushPayload,
    credentials: Annotated[HTTPAuthorizationCredentials, Depends(security)],
    db: AsyncSession = Depends(get_db),
):
    api_key_obj, user = await verify_api_key(credentials, db)
    
    allowed, retry_after = await check_rate_limit(f"ratelimit:{api_key_obj.id}")
    if not allowed:
        raise HTTPException(
            status_code=429,
            detail="Rate limit exceeded",
            headers={"Retry-After": str(retry_after)}
        )
    
    if payload.target.type == "component" and "/" not in payload.target.value:
        raise HTTPException(
            status_code=422,
            detail="invalid_target",
            message="component target requires 'package/activity' format"
        )
    
    if payload.target.type == "none":
        payload.target.value = ""
    
    data_message = {
        "ntf_id": f"ntf_{uuid.uuid4().hex[:12]}",
        "title": payload.title,
        "body": payload.body,
        "icon": payload.icon or "📢",
        "tag": payload.tag or "",
        "priority": payload.priority,
        "target_type": payload.target.type,
        "target_value": payload.target.value,
    }
    
    if payload.devices == ["all"]:
        if api_key_obj.device_id:
            result = await db.execute(select(Device).where(Device.id == api_key_obj.device_id))
        else:
            result = await db.execute(select(Device).where(Device.user_id == api_key_obj.user_id))
        devices = result.scalars().all()
    else:
        device_ids = [UUID(d) for d in payload.devices]
        result = await db.execute(select(Device).where(Device.id.in_(device_ids)))
        devices = result.scalars().all()
    
    if not devices:
        return PushResponse(id=data_message["ntf_id"], status="no_devices", devices_reached=0)
    
    reached = 0
    async def _send_to_device(device):
        try:
            from services.fcm import send_fcm_message
            await asyncio.to_thread(send_fcm_message, device.fcm_token, data_message)
            return True
        except Exception as e:
            logger.error("Failed to send push to device %s: %s", device.id, e)
            return False
    
    results = await asyncio.gather(*[_send_to_device(d) for d in devices])
    reached = sum(1 for r in results if r)
    
    if user and api_key_obj.id:
        log = NotificationLog(
            user_id=user.id,
            api_key_id=api_key_obj.id,
            title=payload.title,
            body=payload.body,
            tag=payload.tag,
            target_type=payload.target.type,
            target_value=payload.target.value,
            status="sent" if reached > 0 else "failed",
        )
        db.add(log)
        await db.commit()
    
    return PushResponse(
        id=data_message["ntf_id"],
        status="sent" if reached > 0 else "failed",
        devices_reached=reached,
    )


@router.post("/push/batch", response_model=BatchPushResponse)
async def send_batch_push(
    payload: BatchPushPayload,
    credentials: Annotated[HTTPAuthorizationCredentials, Depends(security)],
    db: AsyncSession = Depends(get_db),
):
    results = []
    for notification in payload.notifications:
        try:
            result = await send_push(notification, credentials, db)
            results.append(result)
        except HTTPException as e:
            results.append(PushResponse(
                id=f"batch_error_{len(results)}",
                status=f"error: {e.detail}",
                devices_reached=0,
            ))
        except Exception as e:
            logger.error("Batch push error: %s", e)
            results.append(PushResponse(
                id=f"batch_error_{len(results)}",
                status=f"error: {str(e)}",
                devices_reached=0,
            ))
    return BatchPushResponse(results=results)
