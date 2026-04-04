import redis.asyncio as redis
from core.config import get_settings

settings = get_settings()

redis_pool: redis.ConnectionPool | None = None


async def init_redis():
    global redis_pool
    redis_pool = redis.ConnectionPool.from_url(
        settings.redis_url,
        max_connections=20,
        decode_responses=True,
    )


async def get_redis() -> redis.Redis:
    global redis_pool
    if redis_pool is None:
        await init_redis()
    return redis.Redis(connection_pool=redis_pool)


async def close_redis():
    global redis_pool
    if redis_pool:
        await redis_pool.disconnect()
        redis_pool = None
