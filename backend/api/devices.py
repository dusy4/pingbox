from datetime import datetime
from typing import Annotated
from uuid import UUID

from fastapi import APIRouter, Depends, HTTPException, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from pydantic import BaseModel
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select

from core.database import get_db
from core.security import get_current_user, verify_api_key
from models.user import User
from models.api_key import Device

router = APIRouter(prefix="/v1/devices", tags=["devices"])
security = HTTPBearer()


class DeviceRegisterRequest(BaseModel):
    fcm_token: str
    device_name: str | None = None
    platform: str = "android"


class DeviceResponse(BaseModel):
    id: str
    device_name: str | None
    platform: str
    created_at: datetime

    class Config:
        from_attributes = True


class DeviceListResponse(BaseModel):
    devices: list[DeviceResponse]


@router.post("", response_model=DeviceResponse)
async def register_device(
    request: DeviceRegisterRequest,
    credentials: Annotated[HTTPAuthorizationCredentials, Depends(security)],
    db: AsyncSession = Depends(get_db),
):
    # Validate credentials are provided
    if not credentials or not credentials.credentials:
        raise HTTPException(status_code=401, detail="Authorization header required")
    
    api_key_obj, user = await verify_api_key(credentials, db)
    
    if not api_key_obj:
        raise HTTPException(status_code=401, detail="Invalid or revoked API key. Please generate an API key first.")
    
    device = Device(
        user_id=user.id if user else None,
        fcm_token=request.fcm_token,
        device_name=request.device_name,
        platform=request.platform,
    )
    db.add(device)
    await db.commit()
    await db.refresh(device)
    
    if api_key_obj and not api_key_obj.device_id:
        api_key_obj.device_id = device.id
        await db.commit()
    
    return DeviceResponse(
        id=str(device.id),
        device_name=device.device_name,
        platform=device.platform,
        created_at=device.created_at,
    )


@router.get("", response_model=DeviceListResponse)
async def list_devices(
    credentials: Annotated[HTTPAuthorizationCredentials, Depends(security)],
    db: AsyncSession = Depends(get_db),
):
    user = await get_current_user(credentials, db)
    result = await db.execute(select(Device).where(Device.user_id == user.id))
    devices = result.scalars().all()
    
    return DeviceListResponse(
        devices=[
            DeviceResponse(
                id=str(d.id),
                device_name=d.device_name,
                platform=d.platform,
                created_at=d.created_at,
            )
            for d in devices
        ]
    )


@router.delete("/{device_id}")
async def delete_device(
    device_id: UUID,
    credentials: Annotated[HTTPAuthorizationCredentials, Depends(security)],
    db: AsyncSession = Depends(get_db),
):
    # Try JWT auth first, then fall back to API key auth
    try:
        user = await get_current_user(credentials, db)
        result = await db.execute(
            select(Device).where(Device.id == device_id, Device.user_id == user.id)
        )
        device = result.scalar_one_or_none()
    except HTTPException:
        # Try API key auth
        api_key_obj, user = await verify_api_key(credentials, db)
        if not api_key_obj or not user:
            raise HTTPException(status_code=401, detail="Invalid credentials")
        result = await db.execute(
            select(Device).where(Device.id == device_id, Device.user_id == user.id)
        )
        device = result.scalar_one_or_none()
    
    if not device:
        raise HTTPException(status_code=404, detail="Device not found")
    
    await db.delete(device)
    await db.commit()
    return {"status": "deleted"}
