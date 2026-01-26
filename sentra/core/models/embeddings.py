from dataclasses import dataclass, field
from typing import List, Optional

EMBED_DIM = {
    "qwen3-embedding": 1024
}

@dataclass
class Embedding:
    """Embedding data class."""
    vector: List[float]
    text: Optional[str] = None
    id: Optional[str] = None
