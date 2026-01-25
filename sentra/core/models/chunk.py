"""
Chunk models for document segmentation.

This module defines the Chunk model, which is the atomic unit for
vector storage and graph processing.
"""

from enum import Enum
from typing import List, Dict, Any, Optional
from pydantic import BaseModel, Field


class ChunkStrategy(str, Enum):
    """Strategies for chunking documents."""
    RECURSIVE = "recursive"  # Standard recursive character/text splitting
    SEMANTIC = "semantic"    # Semantic-based splitting using embeddings
    GRAPH_ATOMIC = "graph_atomic"  # Atomic units optimized for graph extraction
    STRUCTURE_AWARE = "structure_aware"  # Section-aware splitting


class Chunk(BaseModel):
    """
    Atomic unit of document content for indexing and processing.

    A Chunk is the smallest unit that flows through the knowledge base
    pipeline. It gets vectorized for semantic search and processed for
    entity/relationship extraction.

    Attributes:
        chunk_id: Unique chunk identifier
        doc_id: Source document ID
        section_id: Source section ID (if applicable)
        content_text: Text content for embedding and reading
        token_count: Number of tokens in the chunk
        embedding: Vector embedding (computed during indexing)
        strategy: Chunking strategy used
        metadata: Additional metadata (page numbers, coordinates, etc.)
    """
    chunk_id: str = Field(..., description="Unique chunk identifier")
    doc_id: str = Field(..., description="Source document ID")
    kb_id: str = Field(..., description="知识库所属id")
    section_id: Optional[str] = Field(
        default=None,
        description="Source section ID if applicable"
    )

    # Content
    content_text: str = Field(
        ...,
        description="Text content for embedding and processing"
    )
    token_count: int = Field(
        ...,
        ge=0,
        description="Number of tokens in the chunk"
    )

    # Vector indexing data
    embedding: Optional[List[float]] = Field(
        default=None,
        description="Vector embedding (computed during indexing)"
    )

    # Provenance and metadata
    strategy: ChunkStrategy = Field(
        ...,
        description="Chunking strategy used to create this chunk"
    )
    metadata: Dict[str, Any] = Field(
        default_factory=dict,
        description="Additional metadata (page numbers, coordinates, etc.)"
    )

    class Config:
        json_schema_extra = {
            "example": {
                "chunk_id": "chunk_a1b2c3d4",
                "doc_id": "doc_001",
                "section_id": "section_001",
                "content_text": "This is a sample chunk of text content...",
                "token_count": 256,
                "embedding": None,
                "strategy": "recursive",
                "metadata": {
                    "start_char": 0,
                    "end_char": 1000,
                    "page": 1,
                    "language": "en"
                }
            }
        }

    @property
    def is_embedded(self) -> bool:
        """Check if this chunk has been vectorized."""
        return self.embedding is not None and len(self.embedding) > 0
