"""
Core knowledge base building modules.

This package provides the knowledge base construction pipeline following
the ETL (Extract, Transform, Load) pattern.
"""

from .models import (
    Document,
    Section,
    Chunk,
    Entity,
    Relation,
    Community,
    ContentType,
    ChunkStrategy,
)

from .ingestion import (
    MarkdownParser,
    SplitterFactory,
    RecursiveSplitter,
    StructureAwareSplitter,
)

from .indexing.vector import (
    BaseEmbedder,
    OpenAIEmbedder,
    EmbeddingService,
)

from .indexing.graph import (
    GraphExtractor,
    EntityResolver,
    CommunityDetector,
    CommunitySummarizer,
)

from .storage import (
    BaseVectorStore,
    InMemoryVectorStore,
    MilvusStore,
)

from .pipeline import (
    BuildConfiguration,
    PipelineManager,
    BuildResult,
)

__all__ = [
    # Models
    "Document",
    "Section",
    "Chunk",
    "Entity",
    "Relation",
    "Community",
    "ContentType",
    "ChunkStrategy",

    # Ingestion
    "MarkdownParser",
    "SplitterFactory",
    "RecursiveSplitter",
    "StructureAwareSplitter",

    # Vector Indexing
    "BaseEmbedder",
    "OpenAIEmbedder",
    "EmbeddingService",

    # Graph Indexing
    "GraphExtractor",
    "EntityResolver",
    "CommunityDetector",
    "CommunitySummarizer",

    # Storage
    "BaseVectorStore",
    "InMemoryVectorStore",
    "MilvusStore",

    # Pipeline
    "BuildConfiguration",
    "PipelineManager",
    "BuildResult",
]
