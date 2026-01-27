"""
Pipeline manager for orchestrating knowledge base construction.

This module provides the main orchestrator that coordinates all
stages of the ETL pipeline.
"""
import asyncio
import os
import json
from typing import Optional, Dict, Any, List
from pydantic import BaseModel, Field
from sentra.utils.logger import logger

from sentra.core.models import Chunk, Entity, Relation, Community, ChunkStrategy
from sentra.core.ingestion import MarkdownParser, SplitterFactory
from sentra.core.indexing.vector import EmbeddingService
from sentra.core.models.embeddings import EMBED_DIM
from sentra.core.indexing.graph import GraphExtractor
from sentra.core.storage import InMemoryVectorStore, BaseVectorStore
from sentra.core.llm_server import BaseLLMClient, LLMFactory, BaseEmbedder
from sentra.core.agents import GenerateService
from sentra.utils.common import time_record
from sentra import settings


class BuildConfiguration(BaseModel):
    """
    Configuration for the knowledge base build pipeline.

    Attributes:
        chunk_strategy: Chunking strategy to use
        chunk_size: Maximum chunk size
        chunk_overlap: Overlap between chunks
        embedding_model: Name of embedding model
    """
    chunk_strategy: ChunkStrategy = ChunkStrategy(settings.embeddings.chunk_strategy)
    chunk_size: int = settings.embeddings.chunk_size
    chunk_overlap: int = settings.embeddings.chunk_overlap

    embedding_model: str = settings.embeddings.model_name
    metadata: Dict[str, Any] = Field(default_factory=dict)


class BuildResult(BaseModel):
    """
    Results from the knowledge base build pipeline.

    Attributes:
        kb_id: KnowledgeBase ID
        doc_id: Document ID
        total_chunks: Number of chunks created
        total_entities: Number of entities extracted
        total_edges: Number of edges extracted
        total_qac: Number of question answers extracted
        vector_store: Vector store instance
        graph_data: Dictionary containing entities, edges, and qa
        stats: Additional statistics
    """
    kb_id: str
    doc_id: str
    total_chunks: int
    total_entities: int
    total_edges: int
    total_qac: int
    vector_store: Optional[BaseVectorStore] = None
    graph_data: Dict[str, Any] = Field(default_factory=dict)
    stats: Dict[str, Any] = Field(default_factory=dict)
    model_config = {
        "arbitrary_types_allowed": True
    }


class KnowledgeBasePipelineManager:
    """
    Orchestrates the knowledge base construction pipeline.

    This manager coordinates all stages:
    1. Parsing (Markdown -> Document)
    2. Chunking (Document -> Chunks)
    3. parallel Vector Indexing (Chunks -> Vector Store) and Graph Extraction (Chunks -> Entities + Edges)
    4. Graph Question Answer Generate
    """

    def __init__(
        self,
        config: BuildConfiguration,
        llm_client: Optional[BaseLLMClient] = None,
        embedding_client: Optional[BaseEmbedder] = None,
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
        if vector_store is None:
            dimension = EMBED_DIM.get(settings.embeddings.model_name)
            self.vector_store = InMemoryVectorStore(embedding_dimension=dimension)
        else:
            self.vector_store = vector_store

        # Initialize components
        self.embedder = embedding_client
        self.embedding_service = EmbeddingService(self.embedder)
        self.graph_extractor = GraphExtractor(llm_client=self.llm_client)
        self.gqag_agent = GenerateService(llm_sentra=self.llm_client)

    @time_record
    async def build_knowledge_base(
        self,
        markdown_content: str,
        kb_id: Optional[str] = None,
        doc_id: Optional[str] = None,
        title: Optional[str] = None,
        entity_types=None, entity_types_des=None
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
        logger.info("[1/5] Parsing markdown document...")
        document = MarkdownParser.parse(markdown_content, kb_id=kb_id, doc_id=doc_id, title=title)
        logger.info(f"  - Parsed {len(document.sections)} sections")

        # Step 2: Chunk document
        logger.info("[2/5] Chunking document...")
        splitter = SplitterFactory.create(
            strategy=self.config.chunk_strategy,
            chunk_size=self.config.chunk_size,
            chunk_overlap=self.config.chunk_overlap,
            tokenizer=self.llm_client.tokenizer
        )
        chunks = splitter.split(document, kb_id)
        logger.info(f"  - Created {len(chunks)} chunks")

        # step3-4: 并行处理 向量化和图谱化
        logger.info("[3-4/5] Generating embeddings and extracting graph in parallel...")
        embeddings_task = self.embedding_service.embed_chunks(chunks)
        graph_task = self.graph_extractor.extract(chunks, doc_id, kb_id, entity_types, entity_types_des)
        chunks_with_embeddings, (entities, edges, namespace) = await asyncio.gather(
            embeddings_task,
            graph_task
        )
        await self.vector_store.add_chunks(chunks_with_embeddings)
        logger.info(f"  - Embedded {len(chunks_with_embeddings)} chunks")
        logger.info(f"  - Extracted {len(entities)} entities, {len(edges)} edges")

        # Step 5: Graph Question Answer pair Generate = gqag-agent
        logger.info("[5/5] Graph Question Answer pair Generate...")
        results_aggregated, results_multihop, results_cot = await self.gqag_agent.build(namespace, kb_id)
        save_dir = f"{settings.kg.working_dir}/{kb_id}/{namespace}"
        self._save_qa_pair(save_dir, results_aggregated, results_multihop, results_cot)
        qa_pair = results_aggregated + results_multihop + results_cot

        # Build result
        result = BuildResult(
            kb_id=kb_id,
            doc_id=doc_id,
            total_chunks=len(chunks),
            total_entities=len(entities),
            total_edges=len(edges),
            total_qac=len(qa_pair),
            vector_store=self.vector_store,
            graph_data={
                "entities": entities,
                "edges": edges,
                "qa_pair": qa_pair
            },
            stats={
                "sections": len(document.sections),
                "embedding_dimension": self.embedder.dimension,
                "chunk_strategy": self.config.chunk_strategy
            }
        )

        logger.info(f"Knowledge base build complete: \n{result.total_chunks} chunks;\n"
                   f"{result.total_entities} entities;\n {result.total_edges} relations;\n "
                   f"{result.total_qac} qa-pairs")

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
        # TODO 复用retrieval包

    def _save_qa_pair(self, save_dir, results_aggregated, results_multihop, results_cot):
        os.makedirs(save_dir, exist_ok=True)
        save_path_aggregated = f"{save_dir}/aggregated.json"
        save_path_multihop = f"{save_dir}/multi_hop.json"
        save_path_cot = f"{save_dir}/cot.json"
        with open(save_path_aggregated, 'w', encoding='utf-8') as f:
            json.dump(results_aggregated, f, ensure_ascii=False, indent=2)
        with open(save_path_multihop, 'w', encoding='utf-8') as f:
            json.dump(results_multihop, f, ensure_ascii=False, indent=2)
        with open(save_path_cot, 'w', encoding='utf-8') as f:
            json.dump(results_cot, f, ensure_ascii=False, indent=2)
