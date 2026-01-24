# Knowledge Base Building System

This document describes the new knowledge base construction system implemented in `sentra/core/`.

## Architecture Overview

The system follows an **ETL (Extract, Transform, Load) pipeline** pattern:

```
Markdown Document
    ↓
[Ingestion Layer] → Parse & Chunk
    ↓
Chunks (Atomic Units)
    ↓
[Indexing Layer] → Parallel Processing
    ├─→ Vector Indexing → Embeddings → Vector Store
    └─→ Graph Extraction → Entities + Relations
                          ↓
                    Entity Resolution
                          ↓
                    Community Detection (GraphRAG)
                          ↓
                    Community Summarization
```

## Module Structure

```
sentra/core/
├── models/                  # Pydantic data models
│   ├── document.py         # Document, Section, ContentType
│   ├── chunk.py           # Chunk, ChunkStrategy
│   └── graph_schema.py    # Entity, Relation, Community
│
├── ingestion/              # Data parsing and chunking
│   ├── parser.py          # MarkdownParser
│   └── splitter.py        # RecursiveSplitter, StructureAwareSplitter, etc.
│
├── indexing/              # Index construction
│   ├── vector/           # Vector embeddings
│   │   └── embedder.py   # OpenAIEmbedder, EmbeddingService
│   │
│   └── graph/            # Knowledge graph construction
│       ├── extractor.py  # GraphExtractor (wraps KgBuilder)
│       ├── resolver.py   # EntityResolver
│       ├── clustering.py # CommunityDetector
│       └── summarizer.py # CommunitySummarizer
│
├── storage/              # Data persistence
│   └── vector_store.py  # InMemoryVectorStore, MilvusStore
│
└── pipeline/            # Pipeline orchestration
    └── build_manager.py # PipelineManager, BuildConfiguration
```

## Key Features

### 1. Flexible Chunking Strategies

- **Recursive**: Standard LangChain-style splitting
- **Structure-Aware**: Respects section boundaries (ideal for contracts)
- **Semantic**: Topic-based splitting (using embeddings)
- **Graph-Atomic**: Optimized for graph extraction

### 2. Dual Indexing

**Vector Index**: Semantic search capabilities
- Configurable embedding models
- Batch embedding with progress tracking
- Multiple vector store backends (In-memory, Milvus)

**Graph Index**: Structured knowledge representation
- Entity and relationship extraction (using existing KgBuilder)
- Entity deduplication and resolution
- Community detection for GraphRAG

### 3. GraphRAG Support

Hierarchical question answering through community detection:
- **Local QA**: Entity-relation level
- **Global QA**: Community summary level

Supported algorithms:
- Leiden (high-quality, requires igraph/leidenalg)
- Louvain (via NetworkX)
- Label Propagation (simple, fast)

### 4. Modular Design

Each component can be used independently or customized:
- Custom embedders (extend `BaseEmbedder`)
- Custom vector stores (extend `BaseVectorStore`)
- Custom entity types (configure in GraphExtractor)
- Custom chunking strategies (extend `BaseSplitter`)

## Quick Start

### Basic Usage

```python
import asyncio
from sentra.core import PipelineManager, BuildConfiguration, ChunkStrategy
from sentra.core.llm_server import LLMFactory

async def build_kb():
    # Configure pipeline
    config = BuildConfiguration(
        chunk_strategy=ChunkStrategy.RECURSIVE,
        chunk_size=1024,
        enable_vector_index=True,
        enable_graph_index=True,
        enable_communities=True
    )

    # Initialize
    llm_client = LLMFactory.create_llm_cli()
    pipeline = PipelineManager(config=config, llm_client=llm_client)

    # Build knowledge base
    result = await pipeline.build_knowledge_base(
        markdown_content=content,
        doc_id="doc_001"
    )

    print(f"Entities: {result.total_entities}")
    print(f"Relations: {result.total_relations}")
    print(f"Communities: {result.total_communities}")

asyncio.run(build_kb())
```

### Search

```python
# Search the knowledge base
results = await pipeline.search("What is the contract amount?", top_k=5)

for chunk, score in results:
    print(f"Score: {score:.4f}")
    print(f"Content: {chunk.content_text}")
```

## Integration with Existing Code

The new system integrates with existing Sentra components:

### Reuses Existing Components

- **`KgBuilder`** (`core/knowledge_graph/service.py`): Used by `GraphExtractor`
- **`LLMFactory`** (`core/llm_server/llm_factory.py`): Creates LLM clients
- **Tokenizer**: Used for token-aware chunking
- **Prompts**: Existing KG extraction prompts

### Backward Compatibility

- Existing `knowledge_graph` and `agents` modules continue to work
- New pipeline can coexist with old workflows
- Graph storage uses existing `NetworkXStorage` and `neo4j_importer`

## Advanced Configuration

### Custom Entity Types

```python
from sentra.core.indexing.graph import GraphExtractor

extractor = GraphExtractor(
    llm_client=llm_client,
    entity_types=["Person", "Organization", "Contract", "Amount"],
    relation_types=["SIGNS", "PAYS", "SPECIFIES"]
)
```

### Production Vector Store

```python
from sentra.core.storage import MilvusStore

vector_store = MilvusStore(
    collection_name="production_kb",
    embedding_dimension=1536,
    uri="http://milvus:19530"
)

pipeline = PipelineManager(
    config=config,
    llm_client=llm_client,
    vector_store=vector_store
)
```

### Disable Graph Indexing

```python
config = BuildConfiguration(
    enable_vector_index=True,
    enable_graph_index=False,  # Vector-only search
    enable_communities=False
)
```

## Performance Considerations

1. **Parallel Processing**: Chunks are processed concurrently
2. **Batch Embeddings**: Minimizes API calls
3. **Incremental Building**: Add documents to existing knowledge base
4. **Memory Management**: For large documents, split into smaller parts

## Examples

See `examples/build_knowledge_base.py` for a complete working example.

## Future Enhancements

Potential improvements:
- Incremental updates (update existing knowledge base)
- Additional vector stores (ChromaDB, LanceDB, Pinecone)
- Semantic splitter full implementation
- Distributed processing for very large documents
- Community hierarchy (multi-level clustering)
- Graph visualization tools

## Migration Guide

To migrate from old `KgBuilder` to new pipeline:

**Old way:**
```python
from sentra.core.knowledge_graph import KgBuilder

builder = KgBuilder(llm_sentra=llm_client)
nodes, edges, namespace = await builder.build_graph(content, contract_id)
```

**New way:**
```python
from sentra.core import PipelineManager, BuildConfiguration

config = BuildConfiguration(enable_vector_index=False)  # Graph only
pipeline = PipelineManager(config=config, llm_client=llm_client)

result = await pipeline.build_knowledge_base(content, doc_id="doc_001")
entities = result.graph_data["entities"]
relations = result.graph_data["relations"]
```

The new approach provides:
- ✅ Automatic chunking
- ✅ Entity resolution
- ✅ Community detection
- ✅ Vector indexing (optional)
- ✅ Unified API
- ✅ Better progress tracking

## Questions?

Refer to individual module READMEs:
- `core/models/README.md` - Data models
- `core/ingestion/README.md` - Parsing and chunking
- `core/indexing/README.md` - Vector and graph indexing
- `core/storage/README.md` - Storage backends
- `core/pipeline/README.md` - Pipeline orchestration
