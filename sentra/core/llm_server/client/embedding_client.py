import asyncio
import httpx
from abc import ABC, abstractmethod
from typing import List
from sentra import settings
from sentra.core.models.embeddings import EMBED_DIM

class BaseEmbedder(ABC):
    """Abstract base class for embedding models."""

    @abstractmethod
    async def embed_text(self, text: str) -> List[float]:
        """
        Generate embedding for a single text.

        Args:
            text: Text to embed

        Returns:
            Embedding vector (list of floats)
        """
        pass

    @abstractmethod
    async def embed_batch(self, texts: List[str]) -> List[List[float]]:
        """
        Generate embeddings for multiple texts.

        Args:
            texts: List of texts to embed

        Returns:
            List of embedding vectors
        """
        pass

    @property
    @abstractmethod
    def dimension(self) -> int:
        """Return the embedding dimension."""
        pass

class OpenAIEmbedder(BaseEmbedder):
    """
    Embedder using a local OpenAI-compatible API (vLLM).

    Calls the endpoint: http://172.16.107.15:23334/v1/embeddings
    """

    def __init__(self, batch_size: int = 100):
        self.batch_size = batch_size
        self.endpoint = settings.embeddings.base_url
        self._dimension = None

    async def _call_api(self, texts: List[str]) -> List[List[float]]:
        """Call the embeddings API and wrap results into Embedding objects."""
        payload = {
            "model": settings.embeddings.model_name,
            "input": texts
        }

        headers = {
            "Authorization": f"Bearer {settings.embeddings.api_key}",
            "Content-Type": "application/json"
        }

        async with httpx.AsyncClient(timeout=60) as client:
            resp = await client.post(self.endpoint, json=payload, headers=headers)
            resp.raise_for_status()
            data = resp.json()

        embeddings = [
            item["embedding"]
            for item in data["data"]
        ]
        return embeddings

    async def embed_batch(self, texts: List[str]) -> List[List[float]]:
        """Generate embeddings for multiple texts in parallel batches."""
        batches = [texts[i:i + self.batch_size] for i in range(0, len(texts), self.batch_size)]
        tasks = [self._call_api(batch) for batch in batches]
        results = await asyncio.gather(*tasks)
        all_embeddings = [e for batch_emb in results for e in batch_emb]
        return all_embeddings

    async def embed_text(self, text: str) -> List[float]:
        """Generate embedding for a single text."""
        embeddings = await self._call_api([text])
        return embeddings[0]

    @property
    def dimension(self) -> int:
        """
        Get embedding dimension (cached after first call).
        """
        if self._dimension is None:
            self._dimension = EMBED_DIM.get(settings.embeddings.model_name)
        return self._dimension