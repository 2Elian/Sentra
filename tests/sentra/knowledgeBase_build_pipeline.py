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
from sentra.core.pipeline import (
    KnowledgeBasePipelineManager,
    BuildConfiguration,
)
from sentra.core.llm_server import LLMFactory

from pydantic import BaseModel, Field
import uuid
from typing import Dict, Any, List, Optional, Tuple, Literal, get_args
from enum import Enum
class EntityTypeEnum(str, Enum):
    # 参与方
    ContractParty = "合同主体（甲乙方）"
    RelatedParty = "关联方（担保人、代理人）"
    Person = "自然人"
    Organization = "组织机构"

    # 标的物
    Contract = "合同本身"
    ProductService = "产品或服务"
    RightObligation = "权利或义务"
    IntellectualProperty = "知识产权"

    # 核心条款
    Amount = "金额"
    DateTerm = "日期或期限"
    Location = "地点"
    Condition = "条件"
    BreachClause = "违约条款"

    # 时空与度量
    SpecificTime = "具体时间"
    TimeSpan = "时间段"
    SpecificLocation = "具体地点"
    Currency = "货币"
    Unit = "度量单位"
EntityType = Literal[
    "ContractParty",
    "RelatedParty",
    "Person",
    "Organization",
    "Contract",
    "ProductService",
    "RightObligation",
    "IntellectualProperty",
    "Amount",
    "DateTerm",
    "Location",
    "Condition",
    "BreachClause",
    "SpecificTime",
    "TimeSpan",
    "SpecificLocation",
    "Currency",
    "Unit"
]
ENTITY_DES = {
    e.name: e.value
    for e in EntityTypeEnum
}
ENTITY_LIST = list(get_args(EntityType))


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
    config = BuildConfiguration()
    print(f"  - Chunk Strategy: {config.chunk_strategy}")
    print(f"  - Chunk Size: {config.chunk_size}")

    # Step 2: Initialize LLM client
    print("\n[2] Initializing LLM client...")
    llm_client = LLMFactory.create_llm_cli()
    embedding_client = LLMFactory.create_embedding_model()
    print(f"  - LLM Client created")

    # Step 3: Create pipeline
    print("\n[3] Creating pipeline manager...")
    pipeline = KnowledgeBasePipelineManager(
        config=config,
        llm_client=llm_client,
        embedding_client=embedding_client
    )
    print(f"  - Pipeline manager initialized")

    # Step 4: Build knowledge base
    print("\n[4] Building knowledge base...")
    print("-" * 80)

    result = await pipeline.build_knowledge_base(
        markdown_content=markdown_content,
        kb_id="9eo00123490k",
        doc_id="example_doc_001"
    )

    print("-" * 80)
    print("\n[5] Build Results:")
    print(f"  - Document ID: {result.doc_id}")
    print(f"  - Total Chunks: {result.total_chunks}")
    print(f"  - Total Entities: {result.total_entities}")

    print("\n" + "=" * 80)
    print("Example complete!")
    print("=" * 80)


if __name__ == "__main__":
    asyncio.run(main())
