#!/usr/bin/env python
# -*- coding: utf-8 -*-
# @Time    : 2026/1/13 16:41
# @Author  : lizimo@nuist.edu.cn
# @File    : dependencies.py
# @Description: 全局依赖注入
from typing import Optional, Union

from sentra.core.llm_server import LLMFactory
from sentra.core import (
    KgBuilder,
    GenerateService,
    OCRAgent,
    KnowledgeBasePipelineManager
)

class ProductionWorkflowFactory:
    def __init__(self):
        self.llm_langChain = LLMFactory.create_llm()
        self.llm_sentra = LLMFactory.create_llm_cli()
        self.embed_client = LLMFactory.create_embedding_model()

    async def initialize(self):
        # 这里留空 是因为如果有异步需要初始化 就在这里
        # self.llm_langChain = await LLMFactory.create_llm()
        # self.llm_sentra = await LLMFactory.create_llm_cli()
        pass

    def create_current_contract_graphBuild_workflow(self) -> KgBuilder:
        return KgBuilder(self.llm_sentra)

    def create_md_parser_workflow(self) -> OCRAgent:
        return OCRAgent(llm_cli=self.llm_sentra)

    def create_gqag_agent_workflow(self) -> GenerateService:
        return GenerateService(llm_sentra=self.llm_sentra)

    def create_kb_pipeline_workflow(self, vector_store = None) -> KnowledgeBasePipelineManager:
        return KnowledgeBasePipelineManager(llm_client=self.llm_sentra, embedding_client=self.embed_client, vector_store=vector_store)


_factory: Optional[ProductionWorkflowFactory] = None


async def get_factory() -> ProductionWorkflowFactory:
    global _factory
    if _factory is None:
        _factory = ProductionWorkflowFactory()
        await _factory.initialize()
    return _factory
