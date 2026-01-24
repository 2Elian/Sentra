# Core Data Models

This package defines the fundamental Pydantic models used throughout the Sentra knowledge base system.

## Models

### Document Hierarchy (`document.py`)

- **`Document`**: Complete document with hierarchical sections
- **`Section`**: Document section (typically a markdown heading)
- **`ContentType`**: Enum for content types (TEXT, TABLE, IMAGE_REF)

### Chunk Model (`chunk.py`)

- **`Chunk`**: Atomic unit for vector and graph indexing
- **`ChunkStrategy`**: Enum for chunking strategies (RECURSIVE, SEMANTIC, GRAPH_ATOMIC, STRUCTURE_AWARE)

### Graph Schema (`graph_schema.py`)

- **`Entity`**: Graph node representing an entity (Person, Organization, etc.)
- **`Relation`**: Graph edge representing a relationship between entities
- **`Community`**: Cluster of densely connected entities (for GraphRAG)

## Usage

```python
from sentra.core.models import Document, Chunk, Entity

# Parse a document
document = Document(
    doc_id="doc_001",
    title="Sample Document",
    original_source="sample.md",
    sections=[]
)

# Create a chunk
chunk = Chunk(
    chunk_id="chunk_001",
    doc_id="doc_001",
    content_text="Sample content",
    token_count=256,
    strategy=ChunkStrategy.RECURSIVE
)
```
