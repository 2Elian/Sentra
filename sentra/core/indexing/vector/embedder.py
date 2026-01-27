"""
Embedding models and services for vectorization.

This module provides embedder implementations and a service for
batch embedding of chunks.
"""
from typing import List, Optional, Dict, Any
from sentra.core.models import Chunk
from sentra.core.llm_server import BaseEmbedder

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
            from tqdm import tqdm
            chunks_iter = tqdm(
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
