from datetime import datetime, timezone
from typing import Annotated
from uuid import UUID

from fastapi import APIRouter, Depends, HTTPException, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from pydantic import BaseModel
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select

from core.database import get_db
from core.security import get_current_user, create_api_key
from models.user import User
from models.api_key import APIKey

router = APIRouter(prefix="/v1/keys", tags=["keys"])
security = HTTPBearer()


class APIKeyCreateResponse(BaseModel):
    id: str
    key: str
    label: str | None
    created_at: datetime


class APIKeyResponse(BaseModel):
    id: str
    label: str | None
    created_at: datetime
    revoked_at: datetime | None

    class Config:
        from_attributes = True


class APIKeyListResponse(BaseModel):
    keys: list[APIKeyResponse]


@router.post("", response_model=APIKeyCreateResponse)
async def create_api_key_endpoint(
    credentials: Annotated[HTTPAuthorizationCredentials, Depends(security)],
    db: AsyncSession = Depends(get_db),
    label: str | None = None,
):
    user = await get_current_user(credentials, db)
    
    result = await db.execute(select(APIKey).where(APIKey.user_id == user.id, APIKey.revoked_at.is_(None)))
    existing_keys = result.scalars().all()
    
    raw_key, key_hash = create_api_key()
    
    api_key = APIKey(
        user_id=user.id,
        key_hash=key_hash,
        label=label,
    )
    db.add(api_key)
    await db.commit()
    await db.refresh(api_key)
    
    return APIKeyCreateResponse(
        id=str(api_key.id),
        key=raw_key,
        label=api_key.label,
        created_at=api_key.created_at,
    )


@router.get("", response_model=APIKeyListResponse)
async def list_api_keys(
    credentials: Annotated[HTTPAuthorizationCredentials, Depends(security)],
    db: AsyncSession = Depends(get_db),
):
    user = await get_current_user(credentials, db)
    result = await db.execute(
        select(APIKey).where(APIKey.user_id == user.id, APIKey.revoked_at.is_(None))
    )
    keys = result.scalars().all()
    
    return APIKeyListResponse(
        keys=[
            APIKeyResponse(
                id=str(k.id),
                label=k.label,
                created_at=k.created_at,
                revoked_at=k.revoked_at,
            )
            for k in keys
        ]
    )


@router.delete("/{key_id}")
async def revoke_api_key(
    key_id: UUID,
    credentials: Annotated[HTTPAuthorizationCredentials, Depends(security)],
    db: AsyncSession = Depends(get_db),
):
    user = await get_current_user(credentials, db)
    result = await db.execute(
        select(APIKey).where(APIKey.id == key_id, APIKey.user_id == user.id)
    )
    api_key = result.scalar_one_or_none()
    if not api_key:
        raise HTTPException(status_code=404, detail="API key not found")
    
    api_key.revoked_at = datetime.now(timezone.utc)
    await db.commit()
    return {"status": "revoked"}
