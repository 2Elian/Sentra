#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
Example script demonstrating how to build a knowledge base using the new pipeline.

This script shows:
1. How to configure the pipeline
2. How to build a knowledge base from markdown
3. How to search the knowledge base
4. How to access extracted entities, relations, and communities
"""

import asyncio
import os

# Disable proxies
os.environ['HTTP_PROXY'] = ''
os.environ['HTTPS_PROXY'] = ''
os.environ['http_proxy'] = ''
os.environ['https_proxy'] = ''
os.environ['NO_PROXY'] = '*'
os.environ['no_proxy'] = '*'

from sentra import settings
from sentra.core import (
    PipelineManager,
    BuildConfiguration,
    ChunkStrategy,
)
from sentra.core.llm_server import LLMFactory


async def main():
    """Main example function."""

    # Sample markdown content
    markdown_content = """
# Company Agreement

## 1. Parties

This agreement is made between Acme Corporation and John Doe.

## 2. Terms

Acme Corporation shall provide software development services to John Doe.
The services will be delivered at their headquarters located in New York.

## 3. Payment

John Doe shall pay Acme Corporation the amount of $100,000 for the services.
Payment is due within 30 days of service completion.

## 4. Duration

This agreement shall be effective from January 1, 2024 to December 31, 2024.
    """

    print("=" * 80)
    print("Knowledge Base Building Example")
    print("=" * 80)

    # Step 1: Create configuration
    print("\n[1] Creating build configuration...")
    config = BuildConfiguration(
        chunk_strategy=ChunkStrategy.RECURSIVE,
        chunk_size=512,
        chunk_overlap=50,
        enable_vector_index=True,
        enable_graph_index=True,
        enable_communities=True,
        embedding_model="text-embedding-3-small",
        community_algorithm="leiden"
    )
    print(f"  - Chunk Strategy: {config.chunk_strategy}")
    print(f"  - Chunk Size: {config.chunk_size}")
    print(f"  - Enable Vector Index: {config.enable_vector_index}")
    print(f"  - Enable Graph Index: {config.enable_graph_index}")
    print(f"  - Enable Communities: {config.enable_communities}")

    # Step 2: Initialize LLM client
    print("\n[2] Initializing LLM client...")
    llm_client = LLMFactory.create_llm_cli()
    print(f"  - LLM Client created")

    # Step 3: Create pipeline
    print("\n[3] Creating pipeline manager...")
    pipeline = PipelineManager(
        config=config,
        llm_client=llm_client
    )
    print(f"  - Pipeline manager initialized")

    # Step 4: Build knowledge base
    print("\n[4] Building knowledge base...")
    print("-" * 80)

    result = await pipeline.build_knowledge_base(
        markdown_content=markdown_content,
        doc_id="example_doc_001",
        title="Company Agreement"
    )

    print("-" * 80)
    print("\n[5] Build Results:")
    print(f"  - Document ID: {result.doc_id}")
    print(f"  - Total Chunks: {result.total_chunks}")
    print(f"  - Total Entities: {result.total_entities}")
    print(f"  - Total Relations: {result.total_relations}")
    print(f"  - Total Communities: {result.total_communities}")

    # Step 6: Display extracted entities
    print("\n[6] Sample Entities:")
    entities = result.graph_data.get("entities", [])
    for i, entity in enumerate(entities[:5], 1):
        print(f"  {i}. {entity.name} ({entity.type})")
        print(f"     Description: {entity.description[:80]}...")

    # Step 7: Display extracted relations
    print("\n[7] Sample Relations:")
    relations = result.graph_data.get("relations", [])
    for i, relation in enumerate(relations[:5], 1):
        print(f"  {i}. {relation.source} -> {relation.target}")
        print(f"     Type: {relation.type}")
        print(f"     Description: {relation.description[:80]}...")

    # Step 8: Display communities
    print("\n[8] Communities:")
    communities = result.graph_data.get("communities", [])
    for i, community in enumerate(communities, 1):
        print(f"  {i}. Community {community.community_id}")
        print(f"     Size: {community.size} entities")
        print(f"     Summary: {community.summary[:100]}...")

    # Step 9: Search example
    print("\n[9] Searching knowledge base...")
    query = "What is the payment amount?"
    print(f"  Query: {query}")

    search_results = await pipeline.search(query, top_k=3)

    print(f"  Found {len(search_results)} results:")
    for i, (chunk, score) in enumerate(search_results, 1):
        print(f"\n  Result {i} (Score: {score:.4f}):")
        print(f"  {chunk.content_text[:150]}...")

    print("\n" + "=" * 80)
    print("Example complete!")
    print("=" * 80)


if __name__ == "__main__":
    asyncio.run(main())
