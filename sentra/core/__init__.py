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
    EmbeddingService,
)

from .indexing.graph import (
    GraphExtractor,
    EntityResolver,
)

from .storage import (
    BaseVectorStore,
    InMemoryVectorStore,
    MilvusStore,
)

from .pipeline import (
    BuildConfiguration,
    KnowledgeBasePipelineManager,
    BuildResult,
)

from .llm_server import (
    OpenAIClient,
    OpenAIEmbedder,
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
    "EmbeddingService",

    # Graph Indexing
    "GraphExtractor",
    "EntityResolver",

    # Storage
    "BaseVectorStore",
    "InMemoryVectorStore",
    "MilvusStore",

    # Pipeline
    "BuildConfiguration",
    "KnowledgeBasePipelineManager",
    "BuildResult",

    # llm and embedding
    "OpenAIClient",
    "OpenAIEmbedder"
]
