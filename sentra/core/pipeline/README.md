# Pipeline Orchestrator

The pipeline orchestrator coordinates the entire knowledge base construction process.

## PipelineManager

Main orchestrator that runs all stages of the ETL pipeline.

### Pipeline Stages

1. **Parsing**: Markdown → Document (hierarchical sections)
2. **Chunking**: Document → Chunks (atomic units)
3. **Vector Indexing**: Chunks → Vector embeddings → Vector store
4. **Graph Extraction**: Chunks → Entities + Relations
5. **Entity Resolution**: Deduplicate and merge entities
6. **Community Detection**: (Optional) Detect graph communities
7. **Community Summarization**: (Optional) Generate community summaries

### Usage

```python
from sentra.core.pipeline import PipelineManager, BuildConfiguration
from sentra.core.llm_server import LLMFactory

# Create configuration
config = BuildConfiguration(
    chunk_strategy=ChunkStrategy.RECURSIVE,
    chunk_size=1024,
    chunk_overlap=100,
    enable_vector_index=True,
    enable_graph_index=True,
    enable_communities=True
)

# Initialize pipeline
llm_client = LLMFactory.create_llm_cli()
pipeline = PipelineManager(config=config, llm_client=llm_client)

# Build knowledge base
result = await pipeline.build_knowledge_base(
    markdown_content=content,
    doc_id="doc_001",
    title="My Document"
)

# Access results
print(f"Chunks: {result.total_chunks}")
print(f"Entities: {result.total_entities}")
print(f"Relations: {result.total_relations}")
print(f"Communities: {result.total_communities}")
```

### Search

```python
# Search the knowledge base
results = await pipeline.search("What is the contract amount?", top_k=5)

for chunk, score in results:
    print(f"Score: {score:.4f}")
    print(f"Content: {chunk.content_text[:200]}...")
```

## BuildConfiguration

Configuration options for the pipeline:

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `chunk_strategy` | `ChunkStrategy` | `RECURSIVE` | Chunking strategy to use |
| `chunk_size` | `int` | `1024` | Maximum chunk size |
| `chunk_overlap` | `int` | `100` | Overlap between chunks |
| `enable_vector_index` | `bool` | `True` | Build vector index |
| `enable_graph_index` | `bool` | `True` | Build graph index |
| `enable_communities` | `bool` | `True` | Detect communities (GraphRAG) |
| `embedding_model` | `str` | `"text-embedding-3-small"` | Embedding model name |
| `community_algorithm` | `str` | `"leiden"` | Community detection algorithm |

## BuildResult

Results from the pipeline:

| Field | Type | Description |
|-------|------|-------------|
| `doc_id` | `str` | Document ID |
| `total_chunks` | `int` | Number of chunks created |
| `total_entities` | `int` | Number of unique entities |
| `total_relations` | `int` | Number of unique relations |
| `total_communities` | `int` | Number of communities detected |
| `vector_store` | `BaseVectorStore` | Vector store instance |
| `graph_data` | `dict` | Contains entities, relations, communities |
| `stats` | `dict` | Additional statistics |

## Customization

### Custom Vector Store

```python
from sentra.core.storage import MilvusStore

vector_store = MilvusStore(
    collection_name="my_kb",
    embedding_dimension=1536
)

pipeline = PipelineManager(
    config=config,
    llm_client=llm_client,
    vector_store=vector_store
)
```

### Custom Entity Types

```python
from sentra.core.indexing.graph import GraphExtractor

extractor = GraphExtractor(
    llm_client=llm_client,
    entity_types=["Person", "Organization", "Location"],
    relation_types=["WORKS_FOR", "LOCATED_IN"]
)

pipeline.graph_extractor = extractor
```

## Performance Tips

1. **Parallel Processing**: The pipeline automatically parallelizes chunk processing
2. **Batch Embeddings**: Embeddings are generated in batches to optimize API calls
3. **Incremental Building**: You can build knowledge bases incrementally by calling `build_knowledge_base()` multiple times
4. **Memory Management**: For large documents, consider splitting them into smaller parts

## Integration with Existing Code

The pipeline orchestrator integrates with existing Sentra components:
- Uses `LLMFactory` for LLM clients
- Wraps `KgBuilder` for graph extraction
- Compatible with existing tokenizer and embedding logic
