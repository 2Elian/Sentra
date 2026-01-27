"""
Tests for sentra.core.indexing module (vector and graph indexing).

Run tests:
    pytest tests/test_indexing.py -v
    pytest tests/test_indexing.py::TestEmbeddingService -v
    pytest tests/test_indexing.py::TestGraphExtractor -v
    pytest tests/test_indexing.py::TestCommunityDetector -v
"""

import pytest
from unittest.mock import Mock, patch, AsyncMock, MagicMock
import networkx as nx
from sentra.core.indexing.vector.embedder import EmbeddingService
from sentra.core.indexing.graph.extractor import GraphExtractor
from sentra.core.indexing.graph.clustering import CommunityDetector
from sentra.core.models import Chunk, Entity, Relation, Community


class TestEmbeddingService:
    """Test cases for EmbeddingService class."""

    @pytest.fixture
    def mock_embedder(self):
        """Create a mock embedder."""
        embedder = Mock()
        embedder.embed_batch = AsyncMock(return_value=[[0.1, 0.2, 0.3] * 128])
        embedder.embed_text = AsyncMock(return_value=[0.1, 0.2, 0.3] * 128)
        return embedder

    @pytest.fixture
    def sample_chunks(self):
        """Create sample chunks for testing."""
        chunks = [
            Chunk(
                chunk_id="chunk-1",
                doc_id="doc-1",
                kb_id="kb-1",
                content_text="This is the first chunk of text.",
                token_count=10,
                strategy="recursive",
                metadata={}
            ),
            Chunk(
                chunk_id="chunk-2",
                doc_id="doc-1",
                kb_id="kb-1",
                content_text="This is the second chunk of text.",
                token_count=10,
                strategy="recursive",
                metadata={}
            ),
        ]
        return chunks

    def test_embedding_service_initialization(self, mock_embedder):
        """Test EmbeddingService initialization."""
        service = EmbeddingService(embedder=mock_embedder)

        assert service.embedder == mock_embedder

    @pytest.mark.asyncio
    async def test_embed_chunks(self, mock_embedder, sample_chunks):
        """Test embedding a list of chunks."""
        service = EmbeddingService(embedder=mock_embedder)

        result_chunks = await service.embed_chunks(sample_chunks, show_progress=False)

        assert len(result_chunks) == len(sample_chunks)
        assert all(chunk.embedding is not None for chunk in result_chunks)
        assert len(result_chunks[0].embedding) == 384  # 128 * 3

        # Verify embedder was called
        mock_embedder.embed_batch.assert_called_once()

    @pytest.mark.asyncio
    async def test_embed_query(self, mock_embedder):
        """Test embedding a query string."""
        service = EmbeddingService(embedder=mock_embedder)

        query = "What is the main topic?"
        embedding = await service.embed_query(query)

        assert embedding is not None
        assert len(embedding) == 384

        # Verify embedder was called
        mock_embedder.embed_text.assert_called_once_with(query)


class TestGraphExtractor:
    """Test cases for GraphExtractor class."""

    @pytest.fixture
    def mock_llm_client(self):
        """Create a mock LLM client."""
        llm_client = Mock()
        llm_client.tokenizer = Mock()
        llm_client.tokenizer.count_tokens = Mock(return_value=100)
        llm_client.generate_answer = AsyncMock()
        return llm_client

    @pytest.fixture
    def sample_chunks(self):
        """Create sample chunks for graph extraction."""
        chunks = [
            Chunk(
                chunk_id="chunk-1",
                doc_id="doc-1",
                kb_id="kb-1",
                content_text="Alice is a software engineer at TechCorp.",
                token_count=10,
                strategy="recursive",
                metadata={}
            ),
            Chunk(
                chunk_id="chunk-2",
                doc_id="doc-1",
                kb_id="kb-1",
                content_text="Bob works at TechCorp as a data scientist.",
                token_count=10,
                strategy="recursive",
                metadata={}
            ),
        ]
        return chunks

    def test_graph_extractor_initialization(self, mock_llm_client):
        """Test GraphExtractor initialization."""
        extractor = GraphExtractor(llm_client=mock_llm_client)

        assert extractor.llm_client == mock_llm_client

    @pytest.mark.asyncio
    async def test_extract_entities_and_relations(self, mock_llm_client, sample_chunks):
        """Test extracting entities and relations from chunks."""
        # Mock the KgBuilder response
        mock_entities = [
            ("Alice", {"entity_type": "Person", "description": "Software engineer", "source_id": "chunk-1"}),
            ("Bob", {"entity_type": "Person", "description": "Data scientist", "source_id": "chunk-2"}),
            ("TechCorp", {"entity_type": "Organization", "description": "Company", "source_id": "chunk-1"})
        ]
        mock_edges = [
            ("Alice", "TechCorp", {"description": "works at", "source_id": "chunk-1"}),
            ("Bob", "TechCorp", {"description": "works at", "source_id": "chunk-2"})
        ]

        with patch('sentra.core.indexing.graph.extractor.KgBuilder') as MockKgBuilder:
            mock_builder_instance = Mock()
            mock_builder_instance.build_graph = AsyncMock(return_value=(mock_entities, mock_edges, "test_namespace"))
            MockKgBuilder.return_value = mock_builder_instance

            extractor = GraphExtractor(llm_client=mock_llm_client)
            entities, edges, namespace = await extractor.extract(
                chunks=sample_chunks,
                doc_id="doc-1",
                kb_id="kb-1"
            )

            assert len(entities) == 3
            assert len(edges) == 2
            assert namespace == "test_namespace"


class TestCommunityDetector:
    """Test cases for CommunityDetector class."""

    @pytest.fixture
    def sample_entities(self):
        """Create sample entities for community detection."""
        return [
            Entity(
                id="entity-1",
                entity_type="Person",
                description="Person 1",
                source_chunk_ids=["chunk-1"]
            ),
            Entity(
                id="entity-2",
                entity_type="Person",
                description="Person 2",
                source_chunk_ids=["chunk-1"]
            ),
            Entity(
                id="entity-3",
                entity_type="Organization",
                description="Organization 1",
                source_chunk_ids=["chunk-2"]
            ),
            Entity(
                id="entity-4",
                entity_type="Organization",
                description="Organization 2",
                source_chunk_ids=["chunk-2"]
            ),
        ]

    @pytest.fixture
    def sample_relations(self):
        """Create sample relations for community detection."""
        return [
            Relation(
                id="rel-1",
                source="entity-1",
                target="entity-3",
                relation_type="works_at",
                description="works at",
                weight=1.0,
                source_chunk_id="chunk-1"
            ),
            Relation(
                id="rel-2",
                source="entity-2",
                target="entity-3",
                relation_type="works_at",
                description="works at",
                weight=1.0,
                source_chunk_id="chunk-1"
            ),
            Relation(
                id="rel-3",
                source="entity-1",
                target="entity-2",
                relation_type="knows",
                description="knows",
                weight=0.5,
                source_chunk_id="chunk-1"
            ),
            Relation(
                id="rel-4",
                source="entity-3",
                target="entity-4",
                relation_type="partner",
                description="partner",
                weight=0.8,
                source_chunk_id="chunk-2"
            ),
        ]

    def test_community_detector_initialization(self):
        """Test CommunityDetector initialization."""
        detector = CommunityDetector(algorithm="label_propagation")

        assert detector.algorithm == "label_propagation"
        assert detector.resolution == 1.0

    def test_community_detector_with_custom_resolution(self):
        """Test CommunityDetector with custom resolution."""
        detector = CommunityDetector(algorithm="leiden", resolution=2.0)

        assert detector.resolution == 2.0

    @pytest.mark.asyncio
    async def test_detect_communities_label_propagation(self, sample_entities, sample_relations):
        """Test community detection using label propagation."""
        detector = CommunityDetector(algorithm="label_propagation")

        communities = await detector.detect_communities(sample_entities, sample_relations)

        assert len(communities) > 0
        assert all(isinstance(c, Community) for c in communities)
        assert all(c.metadata["algorithm"] == "label_propagation" for c in communities)

    @pytest.mark.asyncio
    async def test_detect_communities_louvain(self, sample_entities, sample_relations):
        """Test community detection using Louvain algorithm."""
        detector = CommunityDetector(algorithm="louvain")

        communities = await detector.detect_communities(sample_entities, sample_relations)

        assert len(communities) > 0
        assert all(isinstance(c, Community) for c in communities)

    def test_community_properties(self, sample_entities, sample_relations):
        """Test that detected communities have correct properties."""
        import asyncio

        detector = CommunityDetector(algorithm="label_propagation")
        communities = asyncio.run(detector.detect_communities(sample_entities, sample_relations))

        for community in communities:
            assert community.community_id is not None
            assert community.level == 0
            assert len(community.entity_ids) > 0
            assert len(community.source_chunk_ids) > 0
            assert "size" in community.metadata
            assert community.metadata["size"] == len(community.entity_ids)


class TestGraphIndexingIntegration:
    """Integration tests for graph indexing pipeline."""

    @pytest.mark.asyncio
    async def test_complete_graph_indexing_workflow(self):
        """Test the complete workflow of graph indexing."""
        # This is a simplified integration test
        # In a real scenario, you would need proper LLM configuration

        # Create sample chunks
        chunks = [
            Chunk(
                chunk_id="chunk-1",
                doc_id="doc-1",
                kb_id="kb-1",
                content_text="Alice is a software engineer at TechCorp.",
                token_count=10,
                strategy="recursive",
                metadata={}
            ),
        ]

        # Create mock LLM client
        mock_llm_client = Mock()
        mock_llm_client.tokenizer = Mock()
        mock_llm_client.tokenizer.count_tokens = Mock(return_value=100)
        mock_llm_client.generate_answer = AsyncMock()

        # Mock extraction response
        mock_entities = [
            ("Alice", {"entity_type": "Person", "description": "Software engineer", "source_id": "chunk-1"}),
            ("TechCorp", {"entity_type": "Organization", "description": "Company", "source_id": "chunk-1"})
        ]
        mock_edges = [
            ("Alice", "TechCorp", {"description": "works at", "source_id": "chunk-1"})
        ]

        with patch('sentra.core.indexing.graph.extractor.KgBuilder') as MockKgBuilder:
            mock_builder_instance = Mock()
            mock_builder_instance.build_graph = AsyncMock(return_value=(mock_entities, mock_edges, "test_ns"))
            MockKgBuilder.return_value = mock_builder_instance

            # Extract entities and relations
            extractor = GraphExtractor(llm_client=mock_llm_client)
            entities, edges, namespace = await extractor.extract(chunks, doc_id="doc-1", kb_id="kb-1")

            assert len(entities) == 2
            assert len(edges) == 1

            # Convert to Entity and Relation objects for community detection
            entity_objects = [
                Entity(
                    id=entity[0],
                    entity_type=entity[1]["entity_type"],
                    description=entity[1]["description"],
                    source_chunk_ids=entity[1]["source_id"].split("<SEP>")
                )
                for entity in entities
            ]

            relation_objects = [
                Relation(
                    id=f"rel-{i}",
                    source=edge[0],
                    target=edge[1],
                    relation_type="related",
                    description=edge[2]["description"],
                    weight=1.0,
                    source_chunk_id=edge[2]["source_id"]
                )
                for i, edge in enumerate(edges)
            ]

            # Detect communities
            detector = CommunityDetector(algorithm="label_propagation")
            communities = await detector.detect_communities(entity_objects, relation_objects)

            assert len(communities) > 0


class TestVectorIndexingIntegration:
    """Integration tests for vector indexing pipeline."""

    @pytest.mark.asyncio
    async def test_complete_vector_indexing_workflow(self):
        """Test the complete workflow of vector indexing."""
        # Create sample chunks
        chunks = [
            Chunk(
                chunk_id="chunk-1",
                doc_id="doc-1",
                kb_id="kb-1",
                content_text="This is the first chunk.",
                token_count=6,
                strategy="recursive",
                metadata={}
            ),
            Chunk(
                chunk_id="chunk-2",
                doc_id="doc-1",
                kb_id="kb-1",
                content_text="This is the second chunk.",
                token_count=6,
                strategy="recursive",
                metadata={}
            ),
        ]

        # Create mock embedder
        mock_embedder = Mock()
        mock_embedder.embed_batch = AsyncMock(return_value=[[0.1, 0.2, 0.3] * 128] * 2)
        mock_embedder.embed_text = AsyncMock(return_value=[0.1, 0.2, 0.3] * 128)

        # Embed chunks
        service = EmbeddingService(embedder=mock_embedder)
        embedded_chunks = await service.embed_chunks(chunks, show_progress=False)

        assert len(embedded_chunks) == 2
        assert all(chunk.embedding is not None for chunk in embedded_chunks)

        # Embed query
        query = "What is this about?"
        query_embedding = await service.embed_query(query)

        assert query_embedding is not None
        assert len(query_embedding) == 384


if __name__ == "__main__":
    pytest.main([__file__, "-v"])
