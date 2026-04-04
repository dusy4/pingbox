from datetime import datetime, timezone
from typing import Annotated
from uuid import UUID

from fastapi import APIRouter, Depends, HTTPException
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from pydantic import BaseModel, Field
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select

from core.database import get_db
from core.security import get_current_user
from models.user import User
from models.notification import Rule

router = APIRouter(prefix="/v1/rules", tags=["rules"])
security = HTTPBearer()


class RuleCreate(BaseModel):
    name: str
    match_tag: str | None = None
    target_type: str = Field(..., pattern="^(url|deeplink|package|component|intent_uri|none)$")
    target_value: str = ""
    priority: int = 0
    enabled: bool = True


class RuleUpdate(BaseModel):
    name: str | None = None
    match_tag: str | None = None
    target_type: str | None = None
    target_value: str | None = None
    priority: int | None = None
    enabled: bool | None = None


class RuleResponse(BaseModel):
    id: str
    name: str
    match_tag: str | None
    target_type: str
    target_value: str
    priority: int
    enabled: bool
    updated_at: datetime

    class Config:
        from_attributes = True


class RulesListResponse(BaseModel):
    rules: list[RuleResponse]


@router.get("", response_model=RulesListResponse)
async def list_rules(
    credentials: Annotated[HTTPAuthorizationCredentials, Depends(security)],
    db: AsyncSession = Depends(get_db),
):
    user = await get_current_user(credentials, db)
    
    result = await db.execute(
        select(Rule).where(Rule.user_id == user.id).order_by(Rule.priority.asc())
    )
    rules = result.scalars().all()
    
    return RulesListResponse(
        rules=[
            RuleResponse(
                id=str(r.id),
                name=r.name,
                match_tag=r.match_tag,
                target_type=r.target_type,
                target_value=r.target_value,
                priority=r.priority,
                enabled=r.enabled,
                updated_at=r.updated_at,
            )
            for r in rules
        ]
    )


@router.post("", response_model=RuleResponse)
async def create_rule(
    rule_data: RuleCreate,
    credentials: Annotated[HTTPAuthorizationCredentials, Depends(security)],
    db: AsyncSession = Depends(get_db),
):
    user = await get_current_user(credentials, db)
    
    rule = Rule(
        user_id=user.id,
        name=rule_data.name,
        match_tag=rule_data.match_tag,
        target_type=rule_data.target_type,
        target_value=rule_data.target_value,
        priority=rule_data.priority,
        enabled=rule_data.enabled,
    )
    db.add(rule)
    await db.commit()
    await db.refresh(rule)
    
    return RuleResponse(
        id=str(rule.id),
        name=rule.name,
        match_tag=rule.match_tag,
        target_type=rule.target_type,
        target_value=rule.target_value,
        priority=rule.priority,
        enabled=rule.enabled,
        updated_at=rule.updated_at,
    )


@router.put("/{rule_id}", response_model=RuleResponse)
async def update_rule(
    rule_id: UUID,
    rule_data: RuleUpdate,
    credentials: Annotated[HTTPAuthorizationCredentials, Depends(security)],
    db: AsyncSession = Depends(get_db),
):
    user = await get_current_user(credentials, db)
    
    result = await db.execute(
        select(Rule).where(Rule.id == rule_id, Rule.user_id == user.id)
    )
    rule = result.scalar_one_or_none()
    if not rule:
        raise HTTPException(status_code=404, detail="Rule not found")
    
    update_data = rule_data.model_dump(exclude_unset=True)
    for field, value in update_data.items():
        setattr(rule, field, value)
    
    rule.updated_at = datetime.now(timezone.utc)
    await db.commit()
    await db.refresh(rule)
    
    return RuleResponse(
        id=str(rule.id),
        name=rule.name,
        match_tag=rule.match_tag,
        target_type=rule.target_type,
        target_value=rule.target_value,
        priority=rule.priority,
        enabled=rule.enabled,
        updated_at=rule.updated_at,
    )


@router.delete("/{rule_id}")
async def delete_rule(
    rule_id: UUID,
    credentials: Annotated[HTTPAuthorizationCredentials, Depends(security)],
    db: AsyncSession = Depends(get_db),
):
    user = await get_current_user(credentials, db)
    
    result = await db.execute(
        select(Rule).where(Rule.id == rule_id, Rule.user_id == user.id)
    )
    rule = result.scalar_one_or_none()
    if not rule:
        raise HTTPException(status_code=404, detail="Rule not found")
    
    await db.delete(rule)
    await db.commit()
    return {"status": "deleted"}
