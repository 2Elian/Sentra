"""
Graph extractor for entity and relationship extraction.

This module wraps the existing KgBuilder functionality to extract
entities and relationships from document chunks.
"""

import uuid
from typing import List, Dict, Any, Tuple
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
        relation_types: List[str] = None,
        max_loop: int = 3
    ):
        """
        Initialize graph extractor.

        Args:
            llm_client: LLM client for extraction
            entity_types: List of entity types to extract (None for default)
            relation_types: List of relation types to extract (None for default)
            max_loop: Maximum refinement loops for extraction
        """
        self.llm_client = llm_client
        self.max_loop = max_loop

        # Use provided types or import defaults
        if entity_types is None:
            from sentra.core.knowledge_graph.models import ENTITY_LIST
            self.entity_types = ENTITY_LIST
        else:
            self.entity_types = entity_types

        if relation_types is None:
            from sentra.core.knowledge_graph.models import RELATIONSHIP_LIST
            self.relation_types = RELATIONSHIP_LIST
        else:
            self.relation_types = relation_types

    async def extract_from_chunk(
        self,
        chunk: Chunk
    ) -> Tuple[List[Entity], List[Relation]]:
        """
        Extract entities and relationships from a single chunk.

        Args:
            chunk: Chunk to process

        Returns:
            Tuple of (entities, relations)
        """
        from sentra.core.knowledge_graph.service import KgBuilder

        # Create a temporary KgBuilder instance
        kg_builder = KgBuilder(llm_sentra=self.llm_client)
        kg_builder.max_loop = self.max_loop

        # Extract using existing logic
        nodes, edges = await kg_builder.local_perception_recognition(chunk)

        # Convert to new models
        entities = []
        for entity_name, entity_list in nodes.items():
            for entity_data in entity_list:
                entities.append(Entity(
                    id=entity_data.get('entity_name', f"E_{uuid.uuid4().hex[:8]}"),
                    name=entity_data.get('entity_name', ''),
                    type=entity_data.get('entity_type', 'Unknown'),
                    description=entity_data.get('description', ''),
                    source_chunk_ids=[chunk.chunk_id]
                ))

        relations = []
        for (src_id, tgt_id), relation_list in edges.items():
            for rel_data in relation_list:
                relations.append(Relation(
                    id=f"R_{uuid.uuid4().hex[:8]}",
                    source=src_id,
                    target=tgt_id,
                    type=rel_data.get('relation_type', 'RELATED_TO'),
                    description=rel_data.get('description', ''),
                    weight=rel_data.get('weight', 1.0),
                    source_chunk_ids=[chunk.chunk_id]
                ))

        return entities, relations

    async def extract_batch(
        self,
        chunks: List[Chunk],
        show_progress: bool = True
    ) -> Tuple[List[Entity], List[Relation]]:
        """
        Extract entities and relationships from multiple chunks.

        Args:
            chunks: List of chunks to process
            show_progress: Whether to show progress bar

        Returns:
            Tuple of (all_entities, all_relations)
        """
        from sentra.utils.run_concurrent import run_concurrent

        if show_progress:
            results = await run_concurrent(
                self.extract_from_chunk,
                chunks,
                desc="Extracting entities and relations",
                unit="chunk"
            )
        else:
            results = [
                await self.extract_from_chunk(chunk)
                for chunk in chunks
            ]

        # Aggregate results
        all_entities = []
        all_relations = []

        for entities, relations in results:
            all_entities.extend(entities)
            all_relations.extend(relations)

        return all_entities, all_relations
