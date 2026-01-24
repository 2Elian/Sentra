"""
Vector store implementations for semantic search.

This module provides abstract interfaces and implementations for
storing and searching vector embeddings.
"""

from abc import ABC, abstractmethod
from typing import List, Optional, Dict, Any, Tuple
from sentra.core.models import Chunk
import numpy as np


class BaseVectorStore(ABC):
    """Abstract base class for vector stores."""

    @abstractmethod
    async def add_chunks(self, chunks: List[Chunk]) -> None:
        """
        Add chunks with embeddings to the store.

        Args:
            chunks: List of chunks to add
        """
        pass

    @abstractmethod
    async def search(
        self,
        query_embedding: List[float],
        top_k: int = 10,
        filters: Optional[Dict[str, Any]] = None
    ) -> List[Tuple[Chunk, float]]:
        """
        Search for similar chunks.

        Args:
            query_embedding: Query vector
            top_k: Number of results to return
            filters: Optional metadata filters

        Returns:
            List of (chunk, score) tuples
        """
        pass

    @abstractmethod
    async def delete_by_doc(self, doc_id: str) -> None:
        """
        Delete all chunks belonging to a document.

        Args:
            doc_id: Document ID to delete
        """
        pass

    @abstractmethod
    async def count(self) -> int:
        """
        Get total number of chunks in the store.

        Returns:
            Total chunk count
        """
        pass


class InMemoryVectorStore(BaseVectorStore):
    """
    In-memory vector store using NumPy for similarity search.

    This is a simple implementation suitable for development and
    small-scale applications. For production, use MilvusStore or
    other persistent vector databases.
    """

    def __init__(self, embedding_dimension: int):
        """
        Initialize in-memory vector store.

        Args:
            embedding_dimension: Dimension of embedding vectors
        """
        self.embedding_dimension = embedding_dimension
        self.chunks: List[Chunk] = []
        self.embeddings: np.ndarray = np.zeros((0, embedding_dimension))

    async def add_chunks(self, chunks: List[Chunk]) -> None:
        """Add chunks with embeddings to the store."""
        for chunk in chunks:
            if chunk.embedding is None:
                continue

            self.chunks.append(chunk)

            # Add embedding to matrix
            new_embedding = np.array([chunk.embedding])
            if len(self.embeddings) == 0:
                self.embeddings = new_embedding
            else:
                self.embeddings = np.vstack([self.embeddings, new_embedding])

    async def search(
        self,
        query_embedding: List[float],
        top_k: int = 10,
        filters: Optional[Dict[str, Any]] = None
    ) -> List[Tuple[Chunk, float]]:
        """
        Search for similar chunks using cosine similarity.

        Args:
            query_embedding: Query vector
            top_k: Number of results to return
            filters: Optional metadata filters

        Returns:
            List of (chunk, score) tuples sorted by score (descending)
        """
        if len(self.chunks) == 0:
            return []

        # Compute cosine similarity
        query_vec = np.array(query_embedding).reshape(1, -1)

        # Normalize
        query_norm = query_vec / (np.linalg.norm(query_vec) + 1e-9)
        embed_norm = self.embeddings / (np.linalg.norm(self.embeddings, axis=1, keepdims=True) + 1e-9)

        # Compute similarities
        similarities = np.dot(embed_norm, query_norm.T).flatten()

        # Get top-k indices
        top_indices = np.argsort(similarities)[::-1][:top_k]

        # Build results
        results = []
        for idx in top_indices:
            chunk = self.chunks[idx]
            score = float(similarities[idx])

            # Apply filters if provided
            if filters:
                match = True
                for key, value in filters.items():
                    if key not in chunk.metadata or chunk.metadata[key] != value:
                        match = False
                        break
                if not match:
                    continue

            results.append((chunk, score))

        return results

    async def delete_by_doc(self, doc_id: str) -> None:
        """Delete all chunks belonging to a document."""
        # Filter out chunks from the specified document
        keep_chunks = []
        keep_embeddings = []

        for chunk, embedding in zip(self.chunks, self.embeddings):
            if chunk.doc_id != doc_id:
                keep_chunks.append(chunk)
                keep_embeddings.append(embedding)

        self.chunks = keep_chunks
        self.embeddings = np.array(keep_embeddings) if keep_embeddings else np.zeros((0, self.embedding_dimension))

    async def count(self) -> int:
        """Get total number of chunks in the store."""
        return len(self.chunks)


class MilvusStore(BaseVectorStore):
    """
    Milvus-based vector store for production use.

    This implementation uses Milvus for scalable, persistent
    vector storage and search.
    """

    def __init__(
        self,
        collection_name: str,
        embedding_dimension: int,
        uri: str = "http://localhost:19530",
        token: Optional[str] = None
    ):
        """
        Initialize Milvus store.

        Args:
            collection_name: Name of the Milvus collection
            embedding_dimension: Dimension of embedding vectors
            uri: Milvus server URI
            token: Optional authentication token
        """
        self.collection_name = collection_name
        self.embedding_dimension = embedding_dimension
        self.uri = uri
        self.token = token
        self.collection = None

        try:
            from pymilvus import connections, Collection, FieldSchema, CollectionSchema, DataType
            self.pymilvus = Collection
            self.connections = connections
            self.FieldSchema = FieldSchema
            self.CollectionSchema = CollectionSchema
            self.DataType = DataType
        except ImportError:
            raise ImportError(
                "pymilvus is required for MilvusStore. "
                "Install it with: pip install pymilvus"
            )

        # Initialize connection and collection
        self._initialize_collection()

    def _initialize_collection(self):
        """Initialize Milvus connection and collection."""
        # Connect to Milvus
        self.connections.connect(
            alias="default",
            uri=self.uri,
            token=self.token
        )

        # Define schema
        fields = [
            self.FieldSchema(name="id", dtype=self.DataType.VARCHAR, max_length=256, is_primary=True),
            self.FieldSchema(name="doc_id", dtype=self.DataType.VARCHAR, max_length=256),
            self.FieldSchema(name="embedding", dtype=self.DataType.FLOAT_VECTOR, dim=self.embedding_dimension),
            self.FieldSchema(name="metadata", dtype=self.DataType.JSON),
        ]

        schema = self.CollectionSchema(fields=fields, description=f"Vector store for {self.collection_name}")

        # Create or get collection
        if self.collection_name in [c.name for c in self.pymilvus.list_collections()]:
            self.collection = self.pymilvus(self.collection_name)
        else:
            self.collection = self.pymilvus(name=self.collection_name, schema=schema)
            # Create index
            index_params = {
                "index_type": "IVF_FLAT",
                "metric_type": "COSINE",
                "params": {"nlist": 128}
            }
            self.collection.create_index(field_name="embedding", index_params=index_params)

    async def add_chunks(self, chunks: List[Chunk]) -> None:
        """Add chunks with embeddings to Milvus."""
        if not self.collection:
            self._initialize_collection()

        data = []
        for chunk in chunks:
            if chunk.embedding is None:
                continue

            data.append({
                "id": chunk.chunk_id,
                "doc_id": chunk.doc_id,
                "embedding": chunk.embedding,
                "metadata": chunk.metadata
            })

        if data:
            self.collection.insert(data)
            self.collection.flush()

    async def search(
        self,
        query_embedding: List[float],
        top_k: int = 10,
        filters: Optional[Dict[str, Any]] = None
    ) -> List[Tuple[Chunk, float]]:
        """Search for similar chunks in Milvus."""
        if not self.collection:
            self._initialize_collection()

        self.collection.load()

        # Build search parameters
        search_params = {"metric_type": "COSINE", "params": {"nprobe": 10}}

        # Execute search
        results = self.collection.search(
            data=[query_embedding],
            anns_field="embedding",
            param=search_params,
            limit=top_k,
            expr=None,  # TODO: Add filter expression support
            output_fields=["doc_id", "metadata"]
        )

        # Build results
        output = []
        for hit in results[0]:
            chunk = Chunk(
                chunk_id=hit.id,
                doc_id=hit.entity.get("doc_id"),
                content_text="",  # Not stored in vector store
                token_count=0,
                strategy="recursive",
                metadata=hit.entity.get("metadata", {})
            )
            score = hit.score
            output.append((chunk, score))

        return output

    async def delete_by_doc(self, doc_id: str) -> None:
        """Delete all chunks belonging to a document."""
        if not self.collection:
            return

        self.collection.delete(expr=f"doc_id == '{doc_id}'")

    async def count(self) -> int:
        """Get total number of chunks in the store."""
        if not self.collection:
            return 0
        return self.collection.num_entities
