#!/usr/bin/env python
# -*- coding: utf-8 -*-
# @Time    : 2026/1/13 16:41
# @Author  : lizimo@nuist.edu.cn
# @File    : dependencies.py
# @Description: 全局依赖注入
from abc import ABC, abstractmethod
from typing import Optional, Union

from sentra.core.llm_server import LLMFactory
from sentra.core.knowledge_graph.graph_store import neo4j_importer
from sentra.core.knowledge_graph import KgBuilder
from sentra.core.agents import GenerateService, OCRAgent

class sentraFactory(ABC):

    @abstractmethod
    def create_current_contract_graphBuild_workflow(self) -> KgBuilder:
        raise NotImplementedError
    @abstractmethod
    def create_self_qa_workflow(self) -> GenerateService:
        raise NotImplementedError

class ProductionWorkflowFactory(sentraFactory):
    def __init__(self):
        self.llm_langChain = LLMFactory.create_llm()
        self.llm_sentra = LLMFactory.create_llm_cli()
        self.neo_4j: neo4j_importer
        # embedding model
        # milvus db

    # 如果有异步的self需要传递给后续的router 在这里添加
    async def initialize(self):
        # self.llm_langChain = await LLMFactory.create_llm()
        # self.llm_sentra = await LLMFactory.create_llm_cli()
        pass

    def create_current_contract_graphBuild_workflow(self) -> KgBuilder:
        return KgBuilder(self.llm_sentra)
    def create_self_qa_workflow(self) -> GenerateService:
        return GenerateService(llm_sentra=self.llm_sentra)
    def create_md_parser_workflow(self) -> OCRAgent:
        return OCRAgent(llm_cli=self.llm_sentra)



_factory: Optional[sentraFactory] = None

async def get_factory() -> ProductionWorkflowFactory:
    global _factory
    if _factory is None:
        _factory = ProductionWorkflowFactory()
        await _factory.initialize()
    return _factory

async def get_kgBuilder_async() -> KgBuilder:
    factory = await get_factory()
    return factory.create_current_contract_graphBuild_workflow()
async def get_generatorSerivce_async() -> GenerateService:
    factory = await get_factory()
    return factory.create_self_qa_workflow()
async def get_ocrService_async() -> OCRAgent:
    factory = await get_factory()
    return factory.create_md_parser_workflow()