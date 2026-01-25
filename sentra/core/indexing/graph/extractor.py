"""
Graph extractor for entity and relationship extraction.

This module wraps the existing KgBuilder functionality to extract
entities and relationships from document chunks.
"""

import uuid
from typing import List, Dict, Any, Tuple, Optional
from sentra.core.models import Chunk, Entity, Relation
from sentra.core.llm_server import BaseLLMClient
from sentra.utils.logger import logger


class GraphExtractor:
    """
    Extracts entities and relationships from text chunks.

    This class provides an interface to the existing KgBuilder functionality,
    adapted for the new knowledge base pipeline.
    """

    def __init__(
        self,
        llm_client: BaseLLMClient,
        entity_types: List[str] = None,
        entity_des: List[str] = None,
    ):
        """
        Initialize graph extractor.

        Args:
            llm_client: LLM client for extraction
            entity_types: List of entity types to extract (None for default)
            entity_des: List of entity des to extract (None for default)
        """
        self.llm_client = llm_client

        # Use provided types or import defaults
        if entity_types is None:
            raise ValueError("Entity types not provided")
        else:
            self.entity_types = entity_types

        if entity_des is None:
            raise ValueError("Entity des not provided")
        else:
            self.entity_des = entity_des

    async def extract(
        self,
        chunks: List[Chunk],
        doc_id: Optional[str] = None, kb_id: Optional[str] = None,
        entity_types=None, entity_types_des=None
    ) -> Tuple[
        list[tuple[str, dict]],
        list[tuple[str, str, dict]],
        str
    ]:
        """
        Extract entities and relationships from multiple chunks.

        Args:
            chunks: List of chunks to process
            show_progress: Whether to show progress bar

        Returns:
            Tuple of (all_entities, all_relations)
        """
        from .service import KgBuilder
        # Create a temporary KgBuilder instance
        kg_builder = KgBuilder(llm_sentra=self.llm_client)
        all_entities, all_edges, namespace = await kg_builder.build_graph(chunks, doc_id, kb_id, entity_types, entity_types_des)

        return all_entities, all_edges, namespace
