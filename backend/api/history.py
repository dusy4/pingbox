from datetime import datetime
from typing import Annotated
from uuid import UUID

from fastapi import APIRouter, Depends, HTTPException, Query
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from pydantic import BaseModel
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, func

from core.database import get_db
from core.security import get_current_user
from models.user import User
from models.notification import NotificationLog

router = APIRouter(prefix="/v1/history", tags=["history"])
security = HTTPBearer()


class NotificationResponse(BaseModel):
    id: str
    title: str
    body: str | None
    tag: str | None
    target_type: str | None
    target_value: str | None
    status: str
    created_at: datetime

    class Config:
        from_attributes = True


class HistoryListResponse(BaseModel):
    notifications: list[NotificationResponse]
    total: int
    page: int
    limit: int


@router.get("", response_model=HistoryListResponse)
async def get_history(
    credentials: Annotated[HTTPAuthorizationCredentials, Depends(security)],
    db: AsyncSession = Depends(get_db),
    page: int = Query(default=1, ge=1),
    limit: int = Query(default=50, ge=1, le=100),
    tag: str | None = None,
    since: datetime | None = None,
):
    user = await get_current_user(credentials, db)
    
    query = select(NotificationLog).where(NotificationLog.user_id == user.id)
    
    if tag:
        query = query.where(NotificationLog.tag == tag)
    if since:
        query = query.where(NotificationLog.created_at >= since)
    
    query = query.order_by(NotificationLog.created_at.desc())
    query = query.offset((page - 1) * limit).limit(limit)
    
    result = await db.execute(query)
    notifications = result.scalars().all()
    
    count_query = select(func.count(NotificationLog.id)).where(NotificationLog.user_id == user.id)
    if tag:
        count_query = count_query.where(NotificationLog.tag == tag)
    if since:
        count_query = count_query.where(NotificationLog.created_at >= since)
    
    count_result = await db.execute(count_query)
    total = count_result.scalar()
    
    return HistoryListResponse(
        notifications=[
            NotificationResponse(
                id=str(n.id),
                title=n.title,
                body=n.body,
                tag=n.tag,
                target_type=n.target_type,
                target_value=n.target_value,
                status=n.status,
                created_at=n.created_at,
            )
            for n in notifications
        ],
        total=total,
        page=page,
        limit=limit,
    )


@router.get("/{notification_id}", response_model=NotificationResponse)
async def get_notification(
    notification_id: UUID,
    credentials: Annotated[HTTPAuthorizationCredentials, Depends(security)],
    db: AsyncSession = Depends(get_db),
):
    user = await get_current_user(credentials, db)
    
    result = await db.execute(
        select(NotificationLog).where(
            NotificationLog.id == notification_id,
            NotificationLog.user_id == user.id,
        )
    )
    notification = result.scalar_one_or_none()
    
    if not notification:
        raise HTTPException(status_code=404, detail="Notification not found")
    
    return NotificationResponse(
        id=str(notification.id),
        title=notification.title,
        body=notification.body,
        tag=notification.tag,
        target_type=notification.target_type,
        target_value=notification.target_value,
        status=notification.status,
        created_at=notification.created_at,
    )
