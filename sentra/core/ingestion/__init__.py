"""
Ingestion layer for data processing and chunking.

This module handles parsing, splitting, and preprocessing of documents
before they are indexed into the knowledge base.
"""

from .parser import MarkdownParser
from .splitter import (
    BaseSplitter,
    RecursiveSplitter,
    SemanticSplitter,
    StructureAwareSplitter,
    SplitterFactory,
)

__all__ = [
    "MarkdownParser",
    "BaseSplitter",
    "RecursiveSplitter",
    "SemanticSplitter",
    "StructureAwareSplitter",
    "SplitterFactory",
]
