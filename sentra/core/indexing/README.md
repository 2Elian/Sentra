# Indexing Layer

The indexing layer handles vector and graph index construction for semantic search and knowledge graph capabilities.

## Components

### Vector Indexing (`vector/`)

#### Embedder (`embedder.py`)

Generates vector embeddings for text chunks.

**Classes:**
- **`BaseEmbedder`**: Abstract interface for embedders
- **`OpenAIEmbedder`**: OpenAI or compatible API embeddings
- **`EmbeddingService`**: Batch embedding with progress tracking

**Usage:**
```python
from sentra.core.indexing.vector import OpenAIEmbedder, EmbeddingService

embedder = OpenAIEmbedder(
    model_name="text-embedding-3-small",
    api_key="your-key",
    base_url="https://api.openai.com/v1"
)

service = EmbeddingService(embedder)
chunks_with_embeddings = await service.embed_chunks(chunks)
```

### Graph Indexing (`graph/`)

#### GraphExtractor (`extractor.py`)

Wraps existing `KgBuilder` to extract entities and relationships from chunks.

**Features:**
- Parallel extraction from multiple chunks
- Integrates with existing LLM-based extraction
- Converts to new Pydantic models

**Usage:**
```python
from sentra.core.indexing.graph import GraphExtractor

extractor = GraphExtractor(llm_client=llm_client)
entities, relations = await extractor.extract_batch(chunks)
```

#### EntityResolver (`resolver.py`)

Deduplicates and merges entities that refer to the same real-world object.

**Features:**
- String similarity matching
- Entity type checking
- Relation updates after merging
- Source chunk aggregation

**Usage:**
```python
from sentra.core.indexing.graph import EntityResolver

resolver = EntityResolver(similarity_threshold=0.85)
resolved_entities, resolved_relations = await resolver.resolve(
    entities,
    relations
)
```

#### CommunityDetector (`clustering.py`)

Detects communities (clusters) in the knowledge graph for GraphRAG.

**Supported Algorithms:**
- **Leiden**: High-quality hierarchical clustering (requires `igraph` and `leidenalg`)
- **Louvain**: Fast modularity-based clustering via NetworkX
- **Label Propagation**: Simple propagation-based clustering

**Usage:**
```python
from sentra.core.indexing.graph import CommunityDetector

detector = CommunityDetector(algorithm="leiden", resolution=1.0)
communities = await detector.detect_communities(entities, relations)
```

#### CommunitySummarizer (`summarizer.py`)

Generates summaries for graph communities using LLM.

**Features:**
- Community-level summarization
- Title generation
- Batch processing with progress tracking

**Usage:**
```python
from sentra.core.indexing.graph import CommunitySummarizer

summarizer = CommunitySummarizer(llm_client=llm_client)
communities_with_summaries = await summarizer.summarize_communities(
    communities,
    entities
)
```

## Integration with Existing Code

The graph indexing layer integrates with the existing `KgBuilder` from `core/knowledge_graph/service.py`:
- `GraphExtractor` wraps `KgBuilder.local_perception_recognition()`
- Reuses existing prompts and extraction logic
- Converts outputs to new Pydantic models (Entity, Relation)

## GraphRAG Support

The indexing layer supports GraphRAG's hierarchical approach:
1. Extract entities and relations from chunks
2. Resolve duplicates
3. Detect communities (optional)
4. Generate community summaries (optional)

This enables both local (entity-relation) and global (community-based) question answering.
