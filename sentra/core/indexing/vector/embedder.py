"""
Embedding models and services for vectorization.

This module provides embedder implementations and a service for
batch embedding of chunks.
"""

from abc import ABC, abstractmethod
from typing import List, Optional, Dict, Any
from sentra.core.models import Chunk


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
    OpenAI-based embedder using LangChain.

    Supports OpenAI embeddings or custom endpoints compatible
    with OpenAI's API.
    """

    def __init__(
        self,
        model_name: str = "text-embedding-3-small",
        api_key: Optional[str] = None,
        base_url: Optional[str] = None,
        batch_size: int = 100
    ):
        """
        Initialize OpenAI embedder.

        Args:
            model_name: Name of the embedding model
            api_key: OpenAI API key (or None to use env variable)
            base_url: Custom base URL (for alternative providers)
            batch_size: Number of texts to embed in one batch
        """
        self.model_name = model_name
        self.batch_size = batch_size
        self._dimension = None

        # Lazy import to avoid dependency issues
        try:
            from langchain_openai import OpenAIEmbeddings
            self.client = OpenAIEmbeddings(
                model=model_name,
                api_key=api_key,
                base_url=base_url
            )
        except ImportError:
            raise ImportError(
                "langchain-openai is required for OpenAIEmbedder. "
                "Install it with: pip install langchain-openai"
            )

    async def embed_text(self, text: str) -> List[float]:
        """Generate embedding for a single text."""
        result = await self.client.embed_documents([text])
        return result[0]

    async def embed_batch(self, texts: List[str]) -> List[List[float]]:
        """Generate embeddings for multiple texts in batches."""
        all_embeddings = []

        for i in range(0, len(texts), self.batch_size):
            batch = texts[i:i + self.batch_size]
            embeddings = await self.client.embed_documents(batch)
            all_embeddings.extend(embeddings)

        return all_embeddings

    @property
    def dimension(self) -> int:
        """Get embedding dimension (cached after first call)."""
        if self._dimension is None:
            # Determine dimension from model name
            # This is a simplified mapping
            dimension_map = {
                "text-embedding-3-small": 1536,
                "text-embedding-3-large": 3072,
                "text-embedding-ada-002": 1536,
            }
            self._dimension = dimension_map.get(
                self.model_name,
                1536  # Default dimension
            )
        return self._dimension


class EmbeddingService:
    """
    Service for embedding chunks with progress tracking.

    This service handles batch embedding of chunks with progress
    reporting and error handling.
    """

    def __init__(self, embedder: BaseEmbedder):
        """
        Initialize embedding service.

        Args:
            embedder: Embedder instance to use
        """
        self.embedder = embedder

    async def embed_chunks(
        self,
        chunks: List[Chunk],
        show_progress: bool = True
    ) -> List[Chunk]:
        """
        Embed a list of chunks.

        Args:
            chunks: List of chunks to embed
            show_progress: Whether to show progress bar

        Returns:
            List of chunks with embeddings populated
        """
        if show_progress:
            from tqdm.asyncio import tqdm_async
            chunks_iter = tqdm_async(
                chunks,
                desc="Embedding chunks",
                unit="chunk"
            )
        else:
            chunks_iter = chunks

        # Extract texts
        texts = [chunk.content_text for chunk in chunks_iter]

        # Generate embeddings in batch
        embeddings = await self.embedder.embed_batch(texts)

        # Update chunks with embeddings
        for chunk, embedding in zip(chunks, embeddings):
            chunk.embedding = embedding
            chunk.token_count = len(embedding)  # Approximation

        return chunks

    async def embed_query(self, query: str) -> List[float]:
        """
        Embed a query string for search.

        Args:
            query: Query text

        Returns:
            Query embedding vector
        """
        return await self.embedder.embed_text(query)
