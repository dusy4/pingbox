from datetime import datetime, timezone
import uuid

from sqlalchemy import Column, String, DateTime, Text, ForeignKey, Boolean, Integer, ARRAY
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy import JSON
from sqlalchemy.orm import relationship

from core.database import Base


class NotificationLog(Base):
    __tablename__ = "notifications_log"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    user_id = Column(UUID(as_uuid=True), ForeignKey("users.id", ondelete="CASCADE"), nullable=True)
    api_key_id = Column(UUID(as_uuid=True), ForeignKey("api_keys.id"), nullable=True)
    title = Column(Text, nullable=False)
    body = Column(Text, nullable=True)
    tag = Column(Text, nullable=True)
    target_type = Column(Text, nullable=True)
    target_value = Column(Text, nullable=True)
    status = Column(String(20), default="sent")
    created_at = Column(DateTime(timezone=True), default=lambda: datetime.now(timezone.utc))


class Rule(Base):
    __tablename__ = "rules"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    user_id = Column(UUID(as_uuid=True), ForeignKey("users.id", ondelete="CASCADE"), nullable=True)
    name = Column(Text, nullable=False)
    match_tag = Column(Text, nullable=True)
    target_type = Column(Text, nullable=False)
    target_value = Column(Text, nullable=False)
    priority = Column(Integer, default=0)
    enabled = Column(Boolean, default=True)
    updated_at = Column(DateTime(timezone=True), default=lambda: datetime.now(timezone.utc), onupdate=lambda: datetime.now(timezone.utc))


class Webhook(Base):
    __tablename__ = "webhooks"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    user_id = Column(UUID(as_uuid=True), ForeignKey("users.id", ondelete="CASCADE"), nullable=True)
    label = Column(Text, nullable=True)
    mapping = Column(JSON, nullable=False)
    device_ids = Column(ARRAY(UUID(as_uuid=True)), nullable=True)
    secret = Column(Text, nullable=True)
    created_at = Column(DateTime(timezone=True), default=lambda: datetime.now(timezone.utc))
