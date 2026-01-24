"""
Entity resolver for deduplication and merging.

This module handles entity resolution to merge duplicate entities
that refer to the same real-world entity.
"""

from typing import List, Dict, Tuple
from collections import defaultdict
from sentra.core.models import Entity, Relation
from difflib import SequenceMatcher


class EntityResolver:
    """
    Resolves and merges duplicate entities.

    This class implements entity linking to merge entities that
    refer to the same real-world object (e.g., "Elon Musk" and "Musk").
    """

    def __init__(self, similarity_threshold: float = 0.85):
        """
        Initialize entity resolver.

        Args:
            similarity_threshold: String similarity threshold for merging (0-1)
        """
        self.similarity_threshold = similarity_threshold

    async def resolve(
        self,
        entities: List[Entity],
        relations: List[Relation]
    ) -> Tuple[List[Entity], List[Relation]]:
        """
        Resolve duplicate entities and update relations accordingly.

        Args:
            entities: List of extracted entities
            relations: List of extracted relations

        Returns:
            Tuple of (resolved_entities, resolved_relations)
        """
        # Group entities by name similarity
        entity_groups = await self._group_similar_entities(entities)

        # Merge each group
        resolved_entities = []
        entity_id_mapping = {}  # Maps old_id -> new_id

        for group in entity_groups:
            if len(group) == 1:
                # No duplicates
                entity = group[0]
                resolved_entities.append(entity)
                entity_id_mapping[entity.id] = entity.id
            else:
                # Merge entities
                merged_entity = self._merge_entity_group(group)
                resolved_entities.append(merged_entity)

                # Update mapping
                for entity in group:
                    entity_id_mapping[entity.id] = merged_entity.id

        # Update relations with new entity IDs
        resolved_relations = []
        seen_relations = set()

        for relation in relations:
            # Map entity IDs
            new_source = entity_id_mapping.get(relation.source, relation.source)
            new_target = entity_id_mapping.get(relation.target, relation.target)

            # Skip self-loops created by merging
            if new_source == new_target:
                continue

            # Create updated relation
            updated_relation = Relation(
                id=relation.id,
                source=new_source,
                target=new_target,
                type=relation.type,
                description=relation.description,
                weight=relation.weight,
                source_chunk_ids=relation.source_chunk_ids
            )

            # Deduplicate relations
            relation_key = (new_source, new_target, relation.type)
            if relation_key not in seen_relations:
                seen_relations.add(relation_key)
                resolved_relations.append(updated_relation)
            else:
                # Merge source chunks
                for existing_rel in resolved_relations:
                    if (existing_rel.source == new_source and
                        existing_rel.target == new_target and
                        existing_rel.type == relation.type):
                        existing_rel.source_chunk_ids = list(
                            set(existing_rel.source_chunk_ids + relation.source_chunk_ids)
                        )
                        break

        return resolved_entities, resolved_relations

    async def _group_similar_entities(
        self,
        entities: List[Entity]
    ) -> List[List[Entity]]:
        """
        Group entities by name similarity and type.

        Args:
            entities: List of entities to group

        Returns:
            List of entity groups (each group is a list of similar entities)
        """
        groups = []
        processed = set()

        for entity in entities:
            if entity.id in processed:
                continue

            # Find similar entities
            group = [entity]
            processed.add(entity.id)

            for other in entities:
                if other.id in processed:
                    continue

                # Check if similar
                if (entity.type == other.type and
                    self._are_names_similar(entity.name, other.name)):
                    group.append(other)
                    processed.add(other.id)

            groups.append(group)

        return groups

    def _are_names_similar(self, name1: str, name2: str) -> bool:
        """
        Check if two entity names are similar.

        Args:
            name1: First name
            name2: Second name

        Returns:
            True if names are similar above threshold
        """
        # Calculate string similarity
        similarity = SequenceMatcher(None, name1.lower(), name2.lower()).ratio()

        # Check if one is contained in the other
        contains = (name1.lower() in name2.lower() or
                    name2.lower() in name1.lower())

        return similarity >= self.similarity_threshold or contains

    def _merge_entity_group(self, entities: List[Entity]) -> Entity:
        """
        Merge a group of similar entities into a single entity.

        Args:
            entities: List of similar entities to merge

        Returns:
            Merged entity
        """
        # Use the entity with the most sources as the base
        base_entity = max(entities, key=lambda e: len(e.source_chunk_ids))

        # Merge source chunks
        all_source_chunks = set()
        for entity in entities:
            all_source_chunks.update(entity.source_chunk_ids)

        # Merge descriptions
        unique_descriptions = list(set(e.description for e in entities))
        merged_description = base_entity.description
        if len(unique_descriptions) > 1:
            merged_description = f"{base_entity.description} | " + " | ".join(
                d for d in unique_descriptions if d != base_entity.description
            )

        # Create merged entity
        merged_entity = Entity(
            id=base_entity.id,
            name=base_entity.name,
            type=base_entity.type,
            description=merged_description,
            source_chunk_ids=list(all_source_chunks)
        )

        return merged_entity
