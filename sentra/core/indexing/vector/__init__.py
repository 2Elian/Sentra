"""
Vector indexing module for embedding generation and vector storage.

This module provides embedders and vector indexing services for
semantic search capabilities.
"""

from .embedder import (
    BaseEmbedder,
    OpenAIEmbedder,
    EmbeddingService,
)

__all__ = [
    "BaseEmbedder",
    "OpenAIEmbedder",
    "EmbeddingService",
]
