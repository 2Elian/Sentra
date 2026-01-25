#!/usr/bin/env python
# -*- coding: utf-8 -*-
# @Time    : 2026/1/25 0:39
# @Author  : lizimo@nuist.edu.cn
# @File    : service.py
# @Description: ocr service 作为下层服务 接收java端解析后的content 进一步重构章节段落 对图表进一步做出更细节的处理

from sentra.core.llm_server import BaseLLMClient
from sentra.utils.common import detect_main_language
from sentra.core.templates.kg import RECHORE_MD_TEMPLATE

class OCRAgent:
    def __init__(self, llm_cli: BaseLLMClient):
        self.llm_cli = llm_cli

    async def run(self, md_content: str) -> str:
        language = detect_main_language(md_content)
        prompt = RECHORE_MD_TEMPLATE[language].format(md_content=md_content)
        final_result = await self.llm_cli.generate_answer(prompt) # 我们自己定义的llm cli 会自动过滤掉think内容
        return final_result