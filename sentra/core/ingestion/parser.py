"""
Markdown parser for structured document parsing.

This module provides functionality to parse Markdown documents into
a hierarchical structure of Document -> Sections.
"""

import re
from typing import List, Optional, Dict, Any
from sentra.core.models import Document, Section, ContentType


class MarkdownParser:
    """
    Parser for converting Markdown text into structured Document objects.

    This parser identifies markdown headers (# ## ### etc.) and creates
    a hierarchical section structure while preserving content boundaries.
    """

    # Regex pattern to match markdown headers
    HEADER_PATTERN = re.compile(r'^(#{1,6})\s+(.+)$', re.MULTILINE)

    @classmethod
    def parse(
        cls,
        markdown_content: str,
        doc_id: Optional[str] = None,
        title: Optional[str] = None,
        metadata: Optional[Dict[str, Any]] = None
    ) -> Document:
        """
        Parse markdown content into a structured Document.

        Args:
            markdown_content: Raw markdown text content
            doc_id: Optional document ID (auto-generated if not provided)
            title: Optional document title (derived from first H1 if not provided)
            metadata: Optional metadata dictionary

        Returns:
            Document object with hierarchical sections
        """
        from sentra.utils.common import compute_content_hash

        # Generate doc_id if not provided
        if doc_id is None:
            doc_id = compute_content_hash(markdown_content, prefix="doc-")

        # Extract title from first H1 or use default
        if title is None:
            title = cls._extract_title(markdown_content)

        # Parse sections
        sections = cls._parse_sections(markdown_content, doc_id)

        # Build metadata
        if metadata is None:
            metadata = {}
        metadata.setdefault('total_length', len(markdown_content))
        metadata.setdefault('section_count', len(sections))

        return Document(
            doc_id=doc_id,
            title=title,
            original_source=markdown_content,
            sections=sections,
            metadata=metadata
        )

    @classmethod
    def _extract_title(cls, markdown_content: str) -> str:
        """Extract title from first H1 header or use default."""
        match = re.search(r'^#\s+(.+)$', markdown_content, re.MULTILINE)
        if match:
            return match.group(1).strip()
        return "Untitled Document"

    @classmethod
    def _parse_sections(cls, markdown_content: str, doc_id: str) -> List[Section]:
        """
        Parse markdown content into a list of Sections.

        This method identifies all headers and creates sections with
        proper parent-child relationships.
        """
        sections = []
        lines = markdown_content.split('\n')

        # Track section hierarchy
        section_stack = []  # Stack of (level, section_id) tuples
        section_counter = 0
        current_content = []
        current_level = 0
        current_title = "Root"
        section_counter = 0

        for line in lines:
            header_match = cls.HEADER_PATTERN.match(line)

            if header_match:
                # Save previous section if it has content
                if current_content and current_level > 0:
                    section_id = f"{doc_id}-section-{section_counter}"
                    section_counter += 1

                    # Find parent
                    parent_id = None
                    if section_stack:
                        # Parent is the last section with lower level
                        for level, sid in reversed(section_stack):
                            if level < current_level:
                                parent_id = sid
                                break

                    sections.append(Section(
                        section_id=section_id,
                        title=current_title,
                        level=current_level,
                        content='\n'.join(current_content).strip(),
                        parent_id=parent_id,
                        content_type=ContentType.TEXT
                    ))

                # Start new section
                hashes = header_match.group(1)
                current_level = len(hashes)
                current_title = header_match.group(2).strip()
                current_content = []

                # Update stack (pop sections with same or higher level)
                while section_stack and section_stack[-1][0] >= current_level:
                    section_stack.pop()

            else:
                # Add to current section content
                current_content.append(line)

        # Don't forget the last section
        if current_content and current_level > 0:
            section_id = f"{doc_id}-section-{section_counter}"
            section_counter += 1

            parent_id = None
            if section_stack:
                for level, sid in reversed(section_stack):
                    if level < current_level:
                        parent_id = sid
                        break

            sections.append(Section(
                section_id=section_id,
                title=current_title,
                level=current_level,
                content='\n'.join(current_content).strip(),
                parent_id=parent_id,
                content_type=ContentType.TEXT
            ))

        # If no headers found, create a single root section
        if not sections and markdown_content.strip():
            sections.append(Section(
                section_id=f"{doc_id}-section-0",
                title="Root",
                level=1,
                content=markdown_content.strip(),
                parent_id=None,
                content_type=ContentType.TEXT
            ))

        return sections

    @classmethod
    def parse_file(cls, file_path: str) -> Document:
        """
        Parse a markdown file into a Document object.

        Args:
            file_path: Path to the markdown file

        Returns:
            Document object
        """
        import os
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()

        doc_id = os.path.splitext(os.path.basename(file_path))[0]
        title = doc_id.replace('-', ' ').replace('_', ' ').title()

        return cls.parse(content, doc_id=doc_id, title=title)
