"""
Document hierarchy models for structured content representation.

This module defines the Document and Section models that represent
the hierarchical structure of parsed markdown documents.
"""

from enum import Enum
from typing import List, Dict, Any, Optional
from pydantic import BaseModel, Field


class ContentType(str, Enum):
    """Types of content that can be found in documents."""
    TEXT = "text"
    TABLE = "table"  # OCR Markdown tables
    IMAGE_REF = "image_ref"


class Document(BaseModel):
    """
    Represents a complete document with hierarchical sections.

    Attributes:
        kb_id (str): Unique knowledge base identifier
        doc_id: Unique identifier for the document
        title: Document title
        original_source: Original source file or content
        sections: Flat list of sections (hierarchy maintained via level and parent_id)
        metadata: Additional metadata about the document
    """
    kb_id: str = Field(..., description="Unique knowledgeBase identifier")
    doc_id: str = Field(..., description="Unique document identifier")
    title: str = Field(..., description="Document title")
    original_source: str = Field(..., description="Original source content or file path")
    sections: List['Section'] = Field(
        default_factory=list,
        description="Flat list of sections with hierarchical structure"
    )
    metadata: Dict[str, Any] = Field(
        default_factory=dict,
        description="Additional document metadata"
    )

    class Config:
        json_schema_extra = {
            "example": {
                "doc_id": "doc_001",
                "title": "Contract Agreement",
                "original_source": "/path/to/document.md",
                "sections": [],
                "metadata": {
                    "author": "System",
                    "created_at": "2024-01-01",
                    "total_length": 15000
                }
            }
        }


class Section(BaseModel):
    """
    Represents a section within a document (typically a markdown heading).

    Attributes:
        section_id: Unique identifier for the section
        title: Section title (e.g., heading text)
        level: Header level (1-6 for markdown H1-H6)
        content: Full text content under this section
        parent_id: Parent section ID (None for top-level sections)
        content_type: Type of content (text, table, image_ref)
        metadata: Additional section metadata
    """
    section_id: str = Field(..., description="Unique section identifier")
    title: str = Field(..., description="Section heading/title")
    level: int = Field(..., ge=1, le=6, description="Header level (1-6)")
    content: str = Field(..., description="Section content text")
    parent_id: Optional[str] = Field(
        default=None,
        description="Parent section ID for hierarchical structure"
    )
    content_type: ContentType = Field(
        default=ContentType.TEXT,
        description="Type of content in this section"
    )
    metadata: Dict[str, Any] = Field(
        default_factory=dict,
        description="Additional section metadata"
    )

    class Config:
        json_schema_extra = {
            "example": {
                "section_id": "section_001",
                "title": "1. Introduction",
                "level": 1,
                "content": "This is the introduction content...",
                "parent_id": None,
                "content_type": "text",
                "metadata": {
                    "start_char": 0,
                    "end_char": 500
                }
            }
        }


# Update forward references
Document.model_rebuild()
