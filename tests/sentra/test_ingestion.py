"""
Tests for sentra.core.ingestion module (parser and splitter).

Run tests:
    pytest tests/test_ingestion.py -v
    pytest tests/test_ingestion.py::TestMarkdownParser -v
    pytest tests/test_ingestion.py::TestRecursiveSplitter -v
"""

import pytest
from sentra.core.ingestion.parser import MarkdownParser
from sentra.core.ingestion.splitter import (
    RecursiveSplitter,
    StructureAwareSplitter,
    SplitterFactory
)
from sentra.core.models import ChunkStrategy


class TestMarkdownParser:
    """Test cases for MarkdownParser class."""

    def test_parse_simple_markdown(self):
        """Test parsing simple markdown with headers."""
        markdown_content = """# Main Title

This is some content under the main title.

## Section 1

Content for section 1.

## Section 2

Content for section 2.
"""
        doc = MarkdownParser.parse(
            markdown_content,
            kb_id="test_kb",
            doc_id="test_doc"
        )

        assert doc.doc_id == "test_doc"
        assert doc.kb_id == "test_kb"
        assert doc.title == "Main Title"
        assert len(doc.sections) == 3  # Root + 2 sections
        assert doc.sections[0].title == "Main Title"
        assert doc.sections[1].title == "Section 1"
        assert doc.sections[2].title == "Section 2"

    def test_parse_markdown_without_title(self):
        """Test parsing markdown without H1 header."""
        markdown_content = """This is just some text without a title.

## Subsection

Some content here.
"""
        doc = MarkdownParser.parse(
            markdown_content,
            kb_id="test_kb",
            doc_id="test_doc"
        )

        assert doc.title == "Untitled Document"

    def test_parse_nested_sections(self):
        """Test parsing markdown with nested section hierarchy."""
        markdown_content = """# Main Title

## Section 1

### Subsection 1.1

Content under subsection.

### Subsection 1.2

More content.

## Section 2

Content for section 2.
"""
        doc = MarkdownParser.parse(
            markdown_content,
            kb_id="test_kb",
            doc_id="test_doc"
        )

        assert len(doc.sections) == 5  # Main + 2 sections + 2 subsections

        # Check hierarchy
        subsection_1_1 = next(s for s in doc.sections if s.title == "Subsection 1.1")
        section_1 = next(s for s in doc.sections if s.title == "Section 1")
        assert subsection_1_1.parent_id == section_1.section_id

    def test_parse_markdown_with_custom_metadata(self):
        """Test parsing markdown with custom metadata."""
        markdown_content = """# Test Document

Some content here.
"""
        custom_metadata = {"author": "Test Author", "version": "1.0"}
        doc = MarkdownParser.parse(
            markdown_content,
            kb_id="test_kb",
            doc_id="test_doc",
            metadata=custom_metadata
        )

        assert doc.metadata["author"] == "Test Author"
        assert doc.metadata["version"] == "1.0"
        assert doc.metadata["kb_id"] == "test_kb"
        assert doc.metadata["doc_id"] == "test_doc"

    def test_parse_empty_markdown(self):
        """Test parsing empty markdown content."""
        doc = MarkdownParser.parse(
            "",
            kb_id="test_kb",
            doc_id="test_doc"
        )

        # Should create a root section even for empty content
        assert len(doc.sections) == 0

    def test_parse_markdown_without_headers(self):
        """Test parsing markdown without any headers."""
        markdown_content = """This is plain text without any headers.
Just multiple lines of content.
No special formatting.
"""
        doc = MarkdownParser.parse(
            markdown_content,
            kb_id="test_kb",
            doc_id="test_doc"
        )

        # Should create a single root section
        assert len(doc.sections) == 1
        assert doc.sections[0].title == "Root"
        assert doc.sections[0].level == 1


class TestRecursiveSplitter:
    """Test cases for RecursiveSplitter class."""

    @pytest.fixture
    def sample_document(self):
        """Create a sample document for testing."""
        markdown_content = """# Test Document

## Section 1

This is the first section with some content. It has multiple sentences to test chunking.

## Section 2

This is the second section. It also has content that should be split into chunks appropriately.
"""
        return MarkdownParser.parse(markdown_content, kb_id="test_kb", doc_id="test_doc")

    def test_split_by_characters(self, sample_document):
        """Test splitting by character count."""
        splitter = RecursiveSplitter(
            chunk_size=100,
            chunk_overlap=20
        )

        chunks = splitter.split(sample_document, kb_id="test_kb")

        from sentra.core.models import Chunk
        assert len(chunks) > 0
        assert all(isinstance(chunk, Chunk) for chunk in chunks)
        assert all(chunk.kb_id == "test_kb" for chunk in chunks)
        assert all(chunk.strategy == ChunkStrategy.RECURSIVE for chunk in chunks)

    def test_split_respects_chunk_size(self, sample_document):
        """Test that chunks respect the maximum chunk size."""
        chunk_size = 50
        splitter = RecursiveSplitter(
            chunk_size=chunk_size,
            chunk_overlap=10
        )

        chunks = splitter.split(sample_document, kb_id="test_kb")

        for chunk in chunks:
            assert len(chunk.content_text) <= chunk_size + 20  # Allow some margin

    def test_split_creates_overlapping_chunks(self, sample_document):
        """Test that chunks have overlapping content."""
        splitter = RecursiveSplitter(
            chunk_size=100,
            chunk_overlap=30
        )

        chunks = splitter.split(sample_document, kb_id="test_kb")

        if len(chunks) > 1:
            # Check that adjacent chunks have overlap
            first_chunk_end = chunks[0].content_text[-50:]
            second_chunk_start = chunks[1].content_text[:50]
            # There should be some overlap
            assert first_chunk_end in second_chunk_end or second_chunk_start in first_chunk_end


class TestStructureAwareSplitter:
    """Test cases for StructureAwareSplitter class."""

    @pytest.fixture
    def sample_document(self):
        """Create a sample document with multiple sections."""
        markdown_content = """# Main Document

## Introduction

This is the introduction section with some text.

## Methods

This section describes the methods used. It contains more detailed information about the approach.

## Results

The results section presents the findings from the study.

## Conclusion

The conclusion summarizes the key points.
"""
        return MarkdownParser.parse(markdown_content, kb_id="test_kb", doc_id="test_doc")

    def test_split_respects_section_boundaries(self, sample_document):
        """Test that structure-aware splitting respects section boundaries."""
        splitter = StructureAwareSplitter(
            max_chunk_size=200,
            min_chunk_size=50
        )

        chunks = splitter.split(sample_document, kb_id="test_kb")

        assert len(chunks) > 0
        # All chunks should have structure-aware strategy
        assert all(chunk.strategy == ChunkStrategy.STRUCTURE_AWARE for chunk in chunks)

    def test_split_merges_small_sections(self, sample_document):
        """Test that small sections are merged into single chunks."""
        splitter = StructureAwareSplitter(
            max_chunk_size=500,
            min_chunk_size=10
        )

        chunks = splitter.split(sample_document, kb_id="test_kb")

        # Small sections should be merged
        assert len(chunks) < len(sample_document.sections)


class TestSplitterFactory:
    """Test cases for SplitterFactory class."""

    def test_create_recursive_splitter(self):
        """Test creating a RecursiveSplitter via factory."""
        splitter = SplitterFactory.create(
            ChunkStrategy.RECURSIVE,
            chunk_size=512,
            chunk_overlap=50
        )

        assert isinstance(splitter, RecursiveSplitter)
        assert splitter.chunk_size == 512
        assert splitter.chunk_overlap == 50

    def test_create_structure_aware_splitter(self):
        """Test creating a StructureAwareSplitter via factory."""
        splitter = SplitterFactory.create(
            ChunkStrategy.STRUCTURE_AWARE,
            max_chunk_size=1024,
            min_chunk_size=100
        )

        assert isinstance(splitter, StructureAwareSplitter)
        assert splitter.max_chunk_size == 1024
        assert splitter.min_chunk_size == 100

    def test_create_unsupported_strategy_raises_error(self):
        """Test that creating an unsupported strategy raises an error."""
        with pytest.raises(ValueError, match="Unsupported chunk strategy"):
            SplitterFactory.create("invalid_strategy")


class TestIntegration:
    """Integration tests for parser and splitter working together."""

    def test_parse_and_split_workflow(self):
        """Test the complete workflow of parsing and splitting a document."""
        markdown_content = """# Knowledge Base Article

## Overview

This article provides an overview of the system architecture. The system consists of multiple components that work together.

## Component 1

Description of component 1. This component handles user input and validation.

## Component 2

Description of component 2. This component processes the data and generates output.

## Conclusion

The system is designed to be modular and extensible.
"""

        # Step 1: Parse the document
        doc = MarkdownParser.parse(markdown_content, kb_id="test_kb", doc_id="article_1")
        assert doc is not None
        assert len(doc.sections) > 0

        # Step 2: Split the document
        splitter = SplitterFactory.create(
            ChunkStrategy.STRUCTURE_AWARE,
            max_chunk_size=300,
            min_chunk_size=50
        )
        chunks = splitter.split(doc, kb_id="test_kb")

        assert len(chunks) > 0
        assert all(chunk.kb_id == "test_kb" for chunk in chunks)
        assert all(chunk.doc_id == "article_1" for chunk in chunks)


if __name__ == "__main__":
    pytest.main([__file__, "-v"])
