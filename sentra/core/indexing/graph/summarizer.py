"""
Community summarizer for generating community summaries.

This module generates summaries for graph communities, which is
a key component of GraphRAG for global question answering.
"""

from typing import List, Dict, Any
from sentra.core.models import Entity, Community
from sentra.core.llm_server import BaseLLMClient
from sentra.utils.logger import logger


class CommunitySummarizer:
    """
    Generates summaries for graph communities.

    This class creates community-level summaries that capture
    the main themes and entities in each community.
    """

    def __init__(
        self,
        llm_client: BaseLLMClient,
        summary_prompt_template: str = None
    ):
        """
        Initialize community summarizer.

        Args:
            llm_client: LLM client for summary generation
            summary_prompt_template: Custom prompt template (optional)
        """
        self.llm_client = llm_client
        self.summary_prompt_template = summary_prompt_template or self._default_prompt_template()

    async def summarize_communities(
        self,
        communities: List[Community],
        entities: List[Entity],
        show_progress: bool = True
    ) -> List[Community]:
        """
        Generate summaries for all communities.

        Args:
            communities: List of communities (without summaries)
            entities: List of all entities
            show_progress: Whether to show progress bar

        Returns:
            List of communities with summaries populated
        """
        from sentra.utils.run_concurrent import run_concurrent

        # Create entity lookup
        entity_map = {entity.id: entity for entity in entities}

        async def summarize_single(community: Community) -> Community:
            # Get entities in this community
            community_entities = [
                entity_map[eid]
                for eid in community.entity_ids
                if eid in entity_map
            ]

            # Generate summary
            summary = await self._generate_summary(community, community_entities)

            # Update community
            community.summary = summary
            return community

        if show_progress:
            summarized_communities = await run_concurrent(
                summarize_single,
                communities,
                desc="Summarizing communities",
                unit="community"
            )
        else:
            summarized_communities = [
                await summarize_single(comm)
                for comm in communities
            ]

        return summarized_communities

    async def _generate_summary(
        self,
        community: Community,
        entities: List[Entity]
    ) -> str:
        """
        Generate a summary for a single community.

        Args:
            community: Community to summarize
            entities: Entities in the community

        Returns:
            Community summary text
        """
        # Build entity descriptions
        entity_descriptions = []
        for entity in entities:
            desc = f"- {entity.name} ({entity.type}): {entity.description}"
            entity_descriptions.append(desc)

        entities_text = "\n".join(entity_descriptions)

        # Build prompt
        prompt = self.summary_prompt_template.format(
            community_id=community.community_id,
            entity_count=len(entities),
            entity_descriptions=entities_text
        )

        # Generate summary
        try:
            summary = await self.llm_client.generate_answer(prompt)
            return summary.strip()
        except Exception as e:
            logger.error(f"Failed to generate summary for community {community.community_id}: {e}")
            # Fallback summary
            return f"This community contains {len(entities)} entities including: {', '.join(e.name for e in entities[:5])}"

    def _default_prompt_template(self) -> str:
        """
        Get the default prompt template for community summarization.

        Returns:
            Prompt template string
        """
        return """You are analyzing a knowledge graph community. Please provide a concise summary (2-3 sentences) of this community based on the entities it contains.

Community ID: {community_id}
Number of Entities: {entity_count}

Entities in this community:
{entity_descriptions}

Please provide a summary that:
1. Identifies the main theme or topic of this community
2. Highlights the most important entity types
3. Explains how these entities are related

Summary:"""

    async def generate_title(
        self,
        community: Community,
        entities: List[Entity]
    ) -> str:
        """
        Generate a descriptive title for a community.

        Args:
            community: Community to title
            entities: Entities in the community

        Returns:
            Community title
        """
        # Extract entity names and types
        entity_types = list(set(e.type for e in entities))
        top_entities = entities[:5]

        prompt = f"""Generate a short, descriptive title (max 10 words) for a knowledge graph community with the following characteristics:

Entity Types: {', '.join(entity_types)}
Top Entities: {', '.join(e.name for e in top_entities)}

The title should capture the main theme of this community.

Title:"""

        try:
            title = await self.llm_client.generate_answer(prompt)
            return title.strip().strip('"').strip("'")
        except Exception as e:
            logger.error(f"Failed to generate title for community {community.community_id}: {e}")
            # Fallback title
            return f"{entity_types[0] if entity_types else 'Entity'} Community ({len(entities)} entities)"
