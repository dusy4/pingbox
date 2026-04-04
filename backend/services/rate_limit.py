import time
from functools import lru_cache
from typing import Literal

from core.redis import get_redis
from core.config import get_settings


async def check_rate_limit(key_id: str) -> tuple[bool, int]:
    redis = await get_redis()
    settings = get_settings()
    limit = settings.rate_limit_per_minute
    window = 60
    
    now = time.time()
    window_start = now - window
    
    pipe = redis.pipeline()
    pipe.zremrangebyscore(key_id, 0, window_start)
    pipe.zadd(key_id, {f"{now}:{id(pipe)}": now})
    pipe.zcard(key_id)
    pipe.expire(key_id, window + 10)
    results = await pipe.execute()
    
    request_count = results[2]
    
    if request_count > limit:
        oldest = await redis.zrange(key_id, 0, 0, withscores=True)
        if oldest:
            oldest_time = oldest[0][1]
            retry_after = int(oldest_time + window - now) + 1
            return False, max(1, retry_after)
        return False, window
    
    return True, 0


async def get_rate_limit_remaining(key_id: str) -> int:
    redis = await get_redis()
    settings = get_settings()
    limit = settings.rate_limit_per_minute
    window = 60
    
    now = time.time()
    window_start = now - window
    
    await redis.zremrangebyscore(key_id, 0, window_start)
    count = await redis.zcard(key_id)
    
    return max(0, limit - count)
