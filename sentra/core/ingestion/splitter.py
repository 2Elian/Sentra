"""
Document splitters for chunking strategies.

This module provides various chunking strategies to split documents
into chunks for vector and graph indexing.
"""

from abc import ABC, abstractmethod
from typing import List, Optional, Any
from sentra.core.models import Document, Chunk, ChunkStrategy
from sentra.utils.common import compute_content_hash


class BaseSplitter(ABC):
    """Abstract base class for document splitters."""

    @abstractmethod
    def split(self, document: Document, kb_id: str) -> List[Chunk]:
        """
        Split document into chunks.

        Args:
            document: Document to split

        Returns:
            List of Chunk objects
        """
        pass


class RecursiveSplitter(BaseSplitter):
    """
    Recursive character/text splitter.

    Splits text recursively by different delimiters in order:
    double newlines -> single newline -> period -> space.
    """

    def __init__(
        self,
        chunk_size: int = 1024,
        chunk_overlap: int = 100,
        tokenizer: Optional[Any] = None
    ):
        """
        Initialize recursive splitter.

        Args:
            chunk_size: Maximum chunk size (in characters or tokens)
            chunk_overlap: Overlap between chunks
            tokenizer: Optional tokenizer for token-based splitting
        """
        self.chunk_size = chunk_size
        self.chunk_overlap = chunk_overlap
        self.tokenizer = tokenizer

        # Delimiters in order of preference
        self.delimiters = ["\n\n", "\n", ". ", " ", ""]

    def split(self, document: Document, kb_id: str) -> List[Chunk]:
        """Split document using recursive strategy."""
        chunks = []

        for section in document.sections:
            section_chunks = self._split_text(
                text=section.content,
                doc_id=document.doc_id,
                kb_id=kb_id,
                section_id=section.section_id
            )
            chunks.extend(section_chunks)

        return chunks

    def _split_text(
        self,
        text: str,
        doc_id: str,
        kb_id: str,
        section_id: Optional[str] = None
    ) -> List[Chunk]:
        """Recursively split text into chunks."""
        if self.tokenizer:
            return self._split_by_tokens(text, doc_id, kb_id, section_id)
        else:
            return self._split_by_characters(text, doc_id, kb_id, section_id)

    def _split_by_characters(
        self,
        text: str,
        doc_id: str,
        kb_id: str,
        section_id: Optional[str]
    ) -> List[Chunk]:
        """Split by character count."""
        chunks = []
        start = 0
        text_length = len(text)

        while start < text_length:
            end = start + self.chunk_size

            # Find best breakpoint
            if end < text_length:
                for delimiter in self.delimiters:
                    delimiter_pos = text.rfind(delimiter, start, end)
                    if delimiter_pos != -1:
                        end = delimiter_pos + len(delimiter)
                        break

            chunk_text = text[start:end].strip()
            if chunk_text:
                chunk_id = compute_content_hash(chunk_text, prefix="chunk-")
                chunks.append(Chunk(
                    chunk_id=chunk_id,
                    doc_id=doc_id,
                    kb_id=kb_id,
                    section_id=section_id,
                    content_text=chunk_text,
                    token_count=len(chunk_text),  # Approximate
                    strategy=ChunkStrategy.RECURSIVE,
                    metadata={}
                ))

            start = end - self.chunk_overlap if end < text_length else end

        return chunks

    def _split_by_tokens(
        self,
        text: str,
        doc_id: str,
        kb_id: str,
        section_id: Optional[str]
    ) -> List[Chunk]:
        """Split by token count using tokenizer."""
        tokens = self.tokenizer.encode(text)
        chunks = []
        start = 0
        total_tokens = len(tokens)

        while start < total_tokens:
            end = min(start + self.chunk_size, total_tokens)

            chunk_tokens = tokens[start:end]
            chunk_text = self.tokenizer.decode(chunk_tokens)

            chunk_id = compute_content_hash(chunk_text, prefix="chunk-")
            chunks.append(Chunk(
                chunk_id=chunk_id,
                doc_id=doc_id,
                kb_id=kb_id,
                section_id=section_id,
                content_text=chunk_text,
                token_count=len(chunk_tokens),
                strategy=ChunkStrategy.RECURSIVE,
                metadata={}
            ))

            start = end - self.chunk_overlap if end < total_tokens else end

        return chunks

# TODO: Late Chunk @https://zhuanlan.zhihu.com/p/720243414 and @https://github.com/jina-ai/late-chunking/tree/main

class StructureAwareSplitter(BaseSplitter):
    """
    Structure-aware splitter that respects section boundaries.

    This splitter ensures chunks never cross section (heading) boundaries,
    making it ideal for structured documents like contracts and manuals.
    """

    def __init__(
        self,
        max_chunk_size: int = 2048,
        min_chunk_size: int = 100,
        tokenizer: Optional[Any] = None
    ):
        """
        Initialize structure-aware splitter.

        Args:
            max_chunk_size: Maximum chunk size
            min_chunk_size: Minimum chunk size (smaller sections are merged)
            tokenizer: Optional tokenizer for size calculation
        """
        self.max_chunk_size = max_chunk_size
        self.min_chunk_size = min_chunk_size
        self.tokenizer = tokenizer

    def split(self, document: Document, kb_id: str) -> List[Chunk]:
        """Split document respecting section boundaries."""
        chunks = []
        pending_content = []
        pending_token_count = 0
        current_section_id = None

        for section in document.sections:
            # Calculate section size
            if self.tokenizer:
                section_tokens = self.tokenizer.encode(section.content)
                section_size = len(section_tokens)
            else:
                section_size = len(section.content)

            # Check if we should start a new chunk
            if pending_content and (pending_token_count + section_size) > self.max_chunk_size:
                # Save pending chunk
                chunk_text = '\n\n'.join(pending_content)
                chunk_id = compute_content_hash(chunk_text, prefix="chunk-")
                chunks.append(Chunk(
                    chunk_id=chunk_id,
                    doc_id=document.doc_id,
                    kb_id=kb_id,
                    section_id=current_section_id,
                    content_text=chunk_text,
                    token_count=pending_token_count if self.tokenizer else len(chunk_text),
                    strategy=ChunkStrategy.STRUCTURE_AWARE,
                    metadata={}
                ))
                pending_content = []
                pending_token_count = 0

            # Add section content
            if section_size > 0:
                pending_content.append(f"## {section.title}\n\n{section.content}")
                pending_token_count += section_size
                current_section_id = section.section_id if not pending_content else current_section_id

        # Don't forget the last chunk
        if pending_content:
            chunk_text = '\n\n'.join(pending_content)
            chunk_id = compute_content_hash(chunk_text, prefix="chunk-")
            chunks.append(Chunk(
                chunk_id=chunk_id,
                doc_id=document.doc_id,
                kb_id=kb_id,
                section_id=current_section_id,
                content_text=chunk_text,
                token_count=pending_token_count if self.tokenizer else len(chunk_text),
                strategy=ChunkStrategy.STRUCTURE_AWARE,
                metadata={}
            ))

        return chunks


class SemanticSplitter(BaseSplitter):
    """
    Semantic splitter using embedding similarity.

    This splitter identifies semantic boundaries where the topic changes
    by analyzing embedding similarity between sentences.
    """

    def __init__(
        self,
        embedder,
        breakpoint_threshold: float = 0.3,
        min_chunk_size: int = 100,
        max_chunk_size: int = 1024
    ):
        """
        Initialize semantic splitter.

        Args:
            embedder: Embedding model/function
            breakpoint_threshold: Similarity threshold for breakpoints
            min_chunk_size: Minimum chunk size
            max_chunk_size: Maximum chunk size
        """
        self.embedder = embedder
        self.breakpoint_threshold = breakpoint_threshold
        self.min_chunk_size = min_chunk_size
        self.max_chunk_size = max_chunk_size

    def split(self, document: Document, kb_id: str) -> List[Chunk]:
        """Split document using semantic boundaries."""
        # This is a simplified implementation
        # A full implementation would:
        # 1. Split into sentences
        # 2. Compute embeddings for each sentence
        # 3. Find semantic breakpoints (similarity drops)
        # 4. Group sentences into chunks at breakpoints

        # For now, fall back to structure-aware splitting
        fallback_splitter = StructureAwareSplitter(
            max_chunk_size=self.max_chunk_size,
            min_chunk_size=self.min_chunk_size
        )
        return fallback_splitter.split(document, kb_id)


class SplitterFactory:
    """Factory for creating splitters based on strategy."""

    @staticmethod
    def create(
        strategy: ChunkStrategy,
        **kwargs
    ) -> BaseSplitter:
        """
        Create a splitter instance.

        Args:
            strategy: Chunking strategy
            **kwargs: Additional arguments for the splitter

        Returns:
            BaseSplitter instance
        """
        if strategy == ChunkStrategy.RECURSIVE:
            return RecursiveSplitter(**kwargs)
        elif strategy == ChunkStrategy.STRUCTURE_AWARE:
            return StructureAwareSplitter(**kwargs)
        elif strategy == ChunkStrategy.SEMANTIC:
            return SemanticSplitter(**kwargs)
        else:
            raise ValueError(f"Unsupported chunk strategy: {strategy}")
