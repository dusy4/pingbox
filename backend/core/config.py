from pydantic_settings import BaseSettings
from pydantic import field_validator
from functools import lru_cache


class Settings(BaseSettings):
    database_url: str = "postgresql+asyncpg://triggerapp:triggerapp@localhost:5432/triggerapp"
    redis_url: str = "redis://localhost:6379"
    
    firebase_credentials_path: str = "/run/secrets/firebase.json"
    firebase_credentials_json: str | None = None
    
    jwt_secret: str = "change-me-in-production"
    jwt_algorithm: str = "HS256"
    access_token_expire_minutes: int = 60 * 24 * 7
    
    cors_origins: list[str] = ["http://localhost:3000"]
    
    rate_limit_per_minute: int = 60
    
    @field_validator("jwt_secret")
    @classmethod
    def validate_jwt_secret(cls, v: str) -> str:
        if v == "change-me-in-production":
            raise ValueError(
                "jwt_secret must be set to a secure random value in production. "
                "Set the JWT_SECRET environment variable."
            )
        return v
    
    class Config:
        env_file = ".env"
        extra = "allow"


@lru_cache
def get_settings() -> Settings:
    return Settings()
