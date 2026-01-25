import asyncio
import json
import os
from dataclasses import dataclass
from typing import Generic, TypeVar, Union
T = TypeVar("T")

@dataclass
class StorageNameSpace:
    working_dir: str = None
    namespace: str = None

    async def index_done_callback(self):
        """commit the storage operations after indexing"""

    async def query_done_callback(self):
        """commit the storage operations after querying"""

@dataclass
class BaseListStorage(Generic[T], StorageNameSpace):
    async def all_items(self) -> list[T]:
        raise NotImplementedError

    async def get_by_index(self, index: int) -> Union[T, None]:
        raise NotImplementedError

    async def append(self, data: T):
        raise NotImplementedError

    async def upsert(self, data: list[T]):
        raise NotImplementedError

    async def drop(self):
        raise NotImplementedError


@dataclass
class BaseKVStorage(Generic[T], StorageNameSpace):
    async def all_keys(self) -> list[str]:
        raise NotImplementedError

    async def get_by_id(self, id: str) -> Union[T, None]:
        raise NotImplementedError

    async def get_by_ids(
        self, ids: list[str], fields: Union[set[str], None] = None
    ) -> list[Union[T, None]]:
        raise NotImplementedError

    async def filter_keys(self, data: list[str]) -> set[str]:
        """return un-exist keys"""
        raise NotImplementedError

    async def upsert(self, data: dict[str, T]):
        raise NotImplementedError

    async def drop(self):
        raise NotImplementedError

class JsonKVStorage(BaseKVStorage[T], Generic[T]):
    def __init__(self, file_path: str):
        self.file_path = file_path
        self._lock = asyncio.Lock()
        if not os.path.exists(self.file_path):
            with open(self.file_path, "w", encoding="utf-8") as f:
                json.dump({}, f, ensure_ascii=False)
    async def _load(self) -> dict[str, dict]:
        async with self._lock:
            with open(self.file_path, "r", encoding="utf-8") as f:
                return json.load(f)
    async def _dump(self, data: dict):
        async with self._lock:
            with open(self.file_path, "w", encoding="utf-8") as f:
                json.dump(data, f, ensure_ascii=False, indent=2)

    async def all_keys(self) -> list[str]:
        data = await self._load()
        return list(data.keys())

    async def get_by_id(self, id: str) -> Union[T, None]:
        data = await self._load()
        return data.get(id)

    async def get_by_ids(
        self, ids: list[str], fields: Union[set[str], None] = None
    ) -> list[Union[T, None]]:
        data = await self._load()
        results = []
        for _id in ids:
            item = data.get(_id)
            if item and fields:
                item = {k: v for k, v in item.items() if k in fields}
            results.append(item)
        return results

    async def filter_keys(self, keys: list[str]) -> set[str]:
        data = await self._load()
        return {k for k in keys if k not in data}

    async def upsert(self, data: dict[str, T]):
        store = await self._load()
        store.update(data)
        await self._dump(store)

    async def drop(self):
        async with self._lock:
            with open(self.file_path, "w", encoding="utf-8") as f:
                json.dump({}, f)