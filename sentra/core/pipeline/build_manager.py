"""
Pipeline manager for orchestrating knowledge base construction.

This module provides the main orchestrator that coordinates all
stages of the ETL pipeline.
"""

from typing import Optional, Dict, Any, List
from pydantic import BaseModel, Field
from sentra.utils.logger import logger

from sentra.core.models import Chunk, Entity, Relation, Community, ChunkStrategy
from sentra.core.ingestion import MarkdownParser, SplitterFactory
from sentra.core.indexing.vector import EmbeddingService, OpenAIEmbedder
from sentra.core.indexing.graph import GraphExtractor, EntityResolver, CommunityDetector, CommunitySummarizer
from sentra.core.storage import InMemoryVectorStore, BaseVectorStore
from sentra.core.llm_server import BaseLLMClient, LLMFactory


class BuildConfiguration(BaseModel):
    """
    Configuration for the knowledge base build pipeline.

    Attributes:
        chunk_strategy: Chunking strategy to use
        chunk_size: Maximum chunk size
        chunk_overlap: Overlap between chunks
        enable_vector_index: Whether to build vector index
        enable_graph_index: Whether to build graph index
        enable_communities: Whether to detect communities (GraphRAG)
        embedding_model: Name of embedding model
        community_algorithm: Community detection algorithm
    """
    chunk_strategy: ChunkStrategy = ChunkStrategy.RECURSIVE
    chunk_size: int = 1024
    chunk_overlap: int = 100

    enable_vector_index: bool = True
    enable_graph_index: bool = True
    enable_communities: bool = True

    embedding_model: str = "text-embedding-3-small"
    community_algorithm: str = "leiden"

    metadata: Dict[str, Any] = Field(default_factory=dict)


class BuildResult(BaseModel):
    """
    Results from the knowledge base build pipeline.

    Attributes:
        doc_id: Document ID
        total_chunks: Number of chunks created
        total_entities: Number of entities extracted
        total_relations: Number of relations extracted
        total_communities: Number of communities detected
        vector_store: Vector store instance
        graph_data: Dictionary containing entities, relations, and communities
        stats: Additional statistics
    """
    doc_id: str
    total_chunks: int
    total_entities: int
    total_relations: int
    total_communities: int
    vector_store: Optional[BaseVectorStore] = None
    graph_data: Dict[str, Any] = Field(default_factory=dict)
    stats: Dict[str, Any] = Field(default_factory=dict)


class PipelineManager:
    """
    Orchestrates the knowledge base construction pipeline.

    This manager coordinates all stages:
    1. Parsing (Markdown -> Document)
    2. Chunking (Document -> Chunks)
    3. Vector Indexing (Chunks -> Vector Store)
    4. Graph Extraction (Chunks -> Entities + Relations)
    5. Entity Resolution (Deduplication)
    6. Community Detection (Optional)
    7. Community Summarization (Optional)
    """

    def __init__(
        self,
        config: BuildConfiguration,
        llm_client: Optional[BaseLLMClient] = None,
        vector_store: Optional[BaseVectorStore] = None
    ):
        """
        Initialize pipeline manager.

        Args:
            config: Build configuration
            llm_client: LLM client for extraction and summarization
            vector_store: Vector store instance (created if not provided)
        """
        self.config = config
        self.llm_client = llm_client or LLMFactory.create_llm_cli()

        # Initialize vector store
        if vector_store is None:
            # Determine embedding dimension
            dimension_map = {
                "text-embedding-3-small": 1536,
                "text-embedding-3-large": 3072,
                "text-embedding-ada-002": 1536,
            }
            dimension = dimension_map.get(config.embedding_model, 1536)
            self.vector_store = InMemoryVectorStore(embedding_dimension=dimension)
        else:
            self.vector_store = vector_store

        # Initialize components
        self.embedder = OpenAIEmbedder(
            model_name=config.embedding_model
        )
        self.embedding_service = EmbeddingService(self.embedder)

        if config.enable_graph_index:
            self.graph_extractor = GraphExtractor(llm_client=self.llm_client)
            self.entity_resolver = EntityResolver()

            if config.enable_communities:
                self.community_detector = CommunityDetector(
                    algorithm=config.community_algorithm
                )
                self.community_summarizer = CommunitySummarizer(llm_client=self.llm_client)

    async def build_knowledge_base(
        self,
        markdown_content: str,
        doc_id: Optional[str] = None,
        title: Optional[str] = None
    ) -> BuildResult:
        """
        Build knowledge base from markdown content.

        Args:
            markdown_content: Raw markdown text
            doc_id: Optional document ID
            title: Optional document title

        Returns:
            BuildResult containing all outputs
        """
        logger.info(f"Starting knowledge base build for document: {doc_id or 'unknown'}")

        # Step 1: Parse markdown
        logger.info("[1/6] Parsing markdown document...")
        document = MarkdownParser.parse(markdown_content, doc_id=doc_id, title=title)
        logger.info(f"  - Parsed {len(document.sections)} sections")

        # Step 2: Chunk document
        logger.info("[2/6] Chunking document...")
        splitter = SplitterFactory.create(
            strategy=self.config.chunk_strategy,
            chunk_size=self.config.chunk_size,
            chunk_overlap=self.config.chunk_overlap,
            tokenizer=self.llm_client.tokenizer
        )
        chunks = splitter.split(document)
        logger.info(f"  - Created {len(chunks)} chunks")

        # Step 3: Vector indexing (parallel with graph processing)
        if self.config.enable_vector_index:
            logger.info("[3/6] Generating embeddings...")
            chunks_with_embeddings = await self.embedding_service.embed_chunks(chunks)
            await self.vector_store.add_chunks(chunks_with_embeddings)
            logger.info(f"  - Embedded {len(chunks_with_embeddings)} chunks")
        else:
            chunks_with_embeddings = chunks

        # Step 4: Graph extraction
        entities = []
        relations = []

        if self.config.enable_graph_index:
            logger.info("[4/6] Extracting entities and relations...")
            entities, relations = await self.graph_extractor.extract_batch(chunks_with_embeddings)
            logger.info(f"  - Extracted {len(entities)} entities, {len(relations)} relations")

            # Step 5: Entity resolution
            logger.info("[5/6] Resolving duplicate entities...")
            entities, relations = await self.entity_resolver.resolve(entities, relations)
            logger.info(f"  - After deduplication: {len(entities)} entities, {len(relations)} relations")

        # Step 6: Community detection and summarization
        communities = []

        if self.config.enable_communities and self.config.enable_graph_index:
            logger.info("[6/6] Detecting and summarizing communities...")
            communities = await self.community_detector.detect_communities(entities, relations)
            logger.info(f"  - Detected {len(communities)} communities")

            if communities:
                communities = await self.community_summarizer.summarize_communities(
                    communities,
                    entities
                )
                logger.info(f"  - Generated {len(communities)} community summaries")

        # Build result
        result = BuildResult(
            doc_id=document.doc_id,
            total_chunks=len(chunks),
            total_entities=len(entities),
            total_relations=len(relations),
            total_communities=len(communities),
            vector_store=self.vector_store if self.config.enable_vector_index else None,
            graph_data={
                "entities": entities,
                "relations": relations,
                "communities": communities
            },
            stats={
                "sections": len(document.sections),
                "embedding_dimension": self.embedder.dimension,
                "chunk_strategy": self.config.chunk_strategy
            }
        )

        logger.info(f"Knowledge base build complete: {result.total_chunks} chunks, "
                   f"{result.total_entities} entities, {result.total_relations} relations, "
                   f"{result.total_communities} communities")

        return result

    async def search(
        self,
        query: str,
        top_k: int = 10
    ) -> List[tuple]:
        """
        Search the knowledge base.

        Args:
            query: Search query
            top_k: Number of results to return

        Returns:
            List of (chunk, score) tuples
        """
        query_embedding = await self.embedding_service.embed_query(query)
        results = await self.vector_store.search(query_embedding, top_k=top_k)
        return results
