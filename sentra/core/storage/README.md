# Storage Layer

The storage layer provides abstract interfaces for storing and retrieving vectors and graph data.

## Vector Stores

### BaseVectorStore

Abstract interface for vector storage implementations.

**Methods:**
- `add_chunks(chunks)`: Add chunks with embeddings
- `search(query_embedding, top_k, filters)`: Semantic search
- `delete_by_doc(doc_id)`: Delete all chunks from a document
- `count()`: Get total chunk count

### InMemoryVectorStore

In-memory vector store using NumPy for similarity search.

**Features:**
- Cosine similarity search
- Metadata filtering
- No external dependencies
- Suitable for development and small datasets

**Limitations:**
- Not persistent
- Limited to datasets that fit in memory
- No advanced indexing

**Usage:**
```python
from sentra.core.storage import InMemoryVectorStore

store = InMemoryVectorStore(embedding_dimension=1536)
await store.add_chunks(chunks)
results = await store.search(query_embedding, top_k=10)
```

### MilvusStore

Production-grade vector store using Milvus.

**Features:**
- Persistent storage
- Scalable to millions of vectors
- Advanced indexing (IVF_FLAT, HNSW, etc.)
- Filtered search support

**Requirements:**
```bash
pip install pymilvus
```

**Usage:**
```python
from sentra.core.storage import MilvusStore

store = MilvusStore(
    collection_name="my_kb",
    embedding_dimension=1536,
    uri="http://localhost:19530"
)
await store.add_chunks(chunks)
results = await store.search(query_embedding, top_k=10)
```

## Choosing a Vector Store

| Store | Use Case | Scalability | Persistence |
|-------|----------|-------------|-------------|
| InMemoryVectorStore | Development, small datasets (<10K chunks) | Low | No |
| MilvusStore | Production, large datasets | High | Yes |

## Future Implementations

Additional vector store implementations can be added by extending `BaseVectorStore`:
- ChromaDB
- LanceDB
- Pinecone
- Weaviate
- Qdrant

## Graph Storage

Graph storage currently uses the existing implementations:
- `NetworkXStorage`: In-memory NetworkX graphs
- `neo4j_importer`: Neo4j persistent graph storage

These are located in `core/knowledge_graph/graph_store.py` and are used by the existing `KgBuilder`.
