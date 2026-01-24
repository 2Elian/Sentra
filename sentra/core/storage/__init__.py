"""
Storage abstraction layer for vector and graph stores.

This module provides abstract interfaces and implementations for
storing chunks (vectors) and graph data (entities, relations, communities).
"""

from .vector_store import (
    BaseVectorStore,
    InMemoryVectorStore,
    MilvusStore,
)

__all__ = [
    "BaseVectorStore",
    "InMemoryVectorStore",
    "MilvusStore",
]
