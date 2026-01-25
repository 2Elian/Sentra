import asyncio
from typing import Awaitable, Callable, List, Optional, TypeVar
from tqdm.asyncio import tqdm as tqdm_async
from sentra.utils.logger import logger

T = TypeVar("T")
R = TypeVar("R")

async def run_concurrent(
    coro_fn: Callable[..., Awaitable[R]],
    items: List[T],
    *coro_args,
    desc: str = "processing",
    unit: str = "item",
    **coro_kwargs,
) -> List[R]:
    tasks = [
        asyncio.create_task(coro_fn(it, *coro_args, **coro_kwargs))
        for it in items
    ]

    results = []
    pbar = tqdm_async(total=len(items), desc=desc, unit=unit)

    for future in asyncio.as_completed(tasks):
        try:
            result = await future
            results.append(result)
        except Exception as e:  # pylint: disable=broad-except
            logger.exception("Task failed: %s", e)
            results.append(e)
        finally:
            pbar.update(1)

    pbar.close()

    return [res for res in results if not isinstance(res, Exception)]
