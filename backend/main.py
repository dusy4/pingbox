from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from core.config import get_settings
from core.database import engine, Base
from core.redis import init_redis, close_redis

from api.push import router as push_router
from api.devices import router as devices_router
from api.keys import router as keys_router
from api.auth import router as auth_router
from api.history import router as history_router
from api.rules import router as rules_router
from api.webhook import router as webhook_router

settings = get_settings()


@asynccontextmanager
async def lifespan(app: FastAPI):
    await init_redis()
    
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)
    
    yield
    
    await close_redis()
    await engine.dispose()


app = FastAPI(
    title="TriggerApp API",
    description="Universal notification router for Android",
    version="1.0.0",
    lifespan=lifespan,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(push_router)
app.include_router(devices_router)
app.include_router(keys_router)
app.include_router(auth_router)
app.include_router(history_router)
app.include_router(rules_router)
app.include_router(webhook_router)


@app.get("/health")
async def health_check():
    return {"status": "healthy"}


@app.get("/")
async def root():
    return {"message": "TriggerApp API", "version": "1.0.0"}
