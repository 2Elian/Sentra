"""
Indexing layer for vector and graph index construction.

This module handles embedding generation, vector indexing, and
graph processing for knowledge base construction.
"""

from .vector import (
    EmbeddingService,
)
from .graph import (
    GraphExtractor,
    EntityResolver,
    CommunityDetector,
    CommunitySummarizer,
)

__all__ = [
    "EmbeddingService",
    "GraphExtractor",
    "EntityResolver",
    "CommunityDetector",
    "CommunitySummarizer",
]
