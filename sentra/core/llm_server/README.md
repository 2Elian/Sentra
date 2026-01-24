# sentra-LLM Service

sentra.core.llm_server总共提供了2种大模型的api服务,。一种是基于openai服务自研的llm_api接口，另外一种是基于langchain的openai的服务接口。

## 1. sentra_openai
sentra自研的openai接口提供了更为丰富的功能特性，比如：generate_topk_per_token、generate_answer、generate_inputs_prob

- 使用方法：
```python
from sentra.core.llm_server import LLMFactory
sentra_openai = LLMFactory.create_llm_cli()
```

## 2. 基于langchain的openai接口
保留此接口是为了与langchain/langGraph的提示词模板进行深度适配，便于开发。

- 使用方法：
```python
from sentra.core.llm_server import LLMFactory
sentra_openai = LLMFactory.create_llm()()
```