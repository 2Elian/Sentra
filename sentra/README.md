<div align="center"> 

# sentra: 开发文档

</div>

## TODO1: ocr服务

## TODO2: 知识图谱构建逻辑适配到indexing/graph中

## TODO3: 知识库构建深度适配

## TODO4: selfqa优化

## TODO5: 检索逻辑

首先是Query深度理解(意图识别)：文档语言 --> Query是否需要翻译到文档语言上 --> query分类(映射到知识库上，可以理解为初步的知识库召回) --> Plan模式将query意图拆解成若干个子查询 --> 子查询的react模式，每个子查询会自动调度是否需要执行search和mcp服务

## TODO6：链路中持续收集badcase链