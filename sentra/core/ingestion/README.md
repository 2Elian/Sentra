# Ingestion Layer

The ingestion layer handles parsing and chunking of documents before they are indexed.

## Components

### MarkdownParser (`parser.py`)

Parses markdown documents into a hierarchical structure of `Document -> Sections`.

**Features:**
- Identifies markdown headers (# ## ### etc.)
- Creates hierarchical section structure
- Preserves parent-child relationships
- Handles documents without headers (creates root section)

**Usage:**
```python
from sentra.core.ingestion import MarkdownParser

document = MarkdownParser.parse(markdown_content, doc_id="doc_001")
```

### Splitters (`splitter.py`)

Provides various chunking strategies:

- **`RecursiveSplitter`**: Standard LangChain-style recursive splitting
  - Splits by: `\n\n` → `\n` → `. ` → ` `
  - Respects chunk_size and chunk_overlap
  - Supports token-based or character-based splitting

- **`StructureAwareSplitter`**: Section-aware splitting
  - Never crosses section boundaries
  - Ideal for structured documents (contracts, manuals)
  - Merges small sections to meet minimum size

- **`SemanticSplitter`**: Semantic boundary detection (simplified)
  - Identifies topic changes using embeddings
  - Falls back to structure-aware for now

**Usage:**
```python
from sentra.core.ingestion import SplitterFactory, ChunkStrategy

splitter = SplitterFactory.create(
    strategy=ChunkStrategy.RECURSIVE,
    chunk_size=1024,
    chunk_overlap=100,
    tokenizer=tokenizer_instance
)

chunks = splitter.split(document)
```

## Design Principles

1. **Structure Preservation**: Parsers maintain document hierarchy
2. **Flexibility**: Multiple chunking strategies for different use cases
3. **Token Awareness**: All splitters support token-based sizing
4. **Metadata Tracking**: Source information preserved throughout
