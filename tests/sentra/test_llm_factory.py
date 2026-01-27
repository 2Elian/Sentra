"""
Tests for sentra.core.llm_server.llm_factory module.

Run tests:
    pytest tests/test_llm_factory.py -v
    pytest tests/test_llm_factory.py::TestLLMFactory -v

Note: These tests require proper configuration in configs/sentra/sentra_config.py
"""

import pytest
from unittest.mock import Mock, patch, MagicMock
from sentra.core.llm_server.llm_factory import LLMFactory


class TestLLMFactory:
    """Test cases for LLMFactory class."""

    @pytest.fixture
    def mock_settings(self):
        """Mock settings for testing."""
        mock_settings = Mock()
        mock_settings.llm.provider = "openai"
        mock_settings.llm.model_name = "gpt-4"
        mock_settings.llm.api_key = "test-api-key"
        mock_settings.llm.base_url = "https://api.openai.com/v1"
        mock_settings.llm.temperature = 0.7
        return mock_settings

    def test_create_llm_with_openai_provider(self, mock_settings):
        """Test creating LLM with OpenAI provider."""
        with patch('sentra.core.llm_server.llm_factory.settings', mock_settings):
            llm = LLMFactory.create_llm()

            assert llm is not None
            assert llm.model_name == "gpt-4"
            assert llm.temperature == 0.7

    def test_create_llm_with_custom_temperature(self, mock_settings):
        """Test creating LLM with custom temperature."""
        with patch('sentra.core.llm_server.llm_factory.settings', mock_settings):
            llm = LLMFactory.create_llm(temperature=0.5)

            assert llm.temperature == 0.5

    def test_create_llm_without_settings_raises_error(self):
        """Test that creating LLM without settings raises an error."""
        with patch('sentra.core.llm_server.llm_factory.settings', None):
            with pytest.raises(RuntimeError, match="Configuration not loaded"):
                LLMFactory.create_llm()

    def test_create_llm_cli_with_openai_provider(self, mock_settings):
        """Test creating LLM CLI client with OpenAI provider."""
        with patch('sentra.core.llm_server.llm_factory.settings', mock_settings):
            llm_cli = LLMFactory.create_llm_cli()

            assert llm_cli is not None
            assert llm_cli.model_name == "gpt-4"

    def test_create_llm_cli_without_settings_raises_error(self):
        """Test that creating LLM CLI without settings raises an error."""
        with patch('sentra.core.llm_server.llm_factory.settings', None):
            with pytest.raises(RuntimeError, match="Configuration not loaded"):
                LLMFactory.create_llm_cli()

    def test_create_embedding_model_with_openai_provider(self, mock_settings):
        """Test creating embedding model with OpenAI provider."""
        mock_settings.embeddings.provider = "openai"
        mock_settings.embeddings.api_key = "test-api-key"
        mock_settings.embeddings.base_url = "https://api.openai.com/v1"
        mock_settings.embeddings.model_name = "text-embedding-ada-002"

        with patch('sentra.core.llm_server.llm_factory.settings', mock_settings):
            embedder = LLMFactory.create_embedding_model()

            assert embedder is not None

    def test_create_embedding_model_with_langchain_provider(self, mock_settings):
        """Test creating embedding model with LangChain provider."""
        mock_settings.embeddings.provider = "langchain"
        mock_settings.embeddings.api_key = "test-api-key"
        mock_settings.embeddings.base_url = "https://api.openai.com/v1"
        mock_settings.embeddings.model_name = "text-embedding-ada-002"

        with patch('sentra.core.llm_server.llm_factory.settings', mock_settings):
            embedder = LLMFactory.create_embedding_model()

            assert embedder is not None

    def test_create_embedding_model_without_settings_raises_error(self):
        """Test that creating embedding model without settings raises an error."""
        with patch('sentra.core.llm_server.llm_factory.settings', None):
            with pytest.raises(RuntimeError, match="Configuration not loaded"):
                LLMFactory.create_embedding_model()

    def test_unsupported_llm_provider_raises_error(self, mock_settings):
        """Test that unsupported LLM provider raises an error."""
        mock_settings.llm.provider = "unsupported_provider"

        with patch('sentra.core.llm_server.llm_factory.settings', mock_settings):
            with pytest.raises(ValueError, match="Unsupported LLM provider"):
                LLMFactory.create_llm()

    def test_unsupported_embedding_provider_raises_error(self, mock_settings):
        """Test that unsupported embedding provider raises an error."""
        mock_settings.embeddings.provider = "unsupported_provider"

        with patch('sentra.core.llm_server.llm_factory.settings', mock_settings):
            with pytest.raises(ValueError, match="Unsupported embedding provider"):
                LLMFactory.create_embedding_model()


class TestLLMClientIntegration:
    """Integration tests for LLM client functionality."""

    @pytest.mark.asyncio
    async def test_llm_client_generate_answer(self):
        """Test LLM client generate_answer method."""
        # This test requires a mock or actual LLM client
        # For now, we'll skip it as it requires configuration
        pytest.skip("Requires proper LLM configuration")

    @pytest.mark.asyncio
    async def test_embedding_client_embed_text(self):
        """Test embedding client embed_text method."""
        # This test requires a mock or actual embedding client
        pytest.skip("Requires proper embedding configuration")


class TestLLMFactoryConfiguration:
    """Test cases for LLMFactory configuration handling."""

    def test_llm_factory_respects_model_name_from_config(self):
        """Test that LLMFactory uses model_name from configuration."""
        mock_settings = Mock()
        mock_settings.llm.provider = "openai"
        mock_settings.llm.model_name = "gpt-3.5-turbo"
        mock_settings.llm.api_key = "test-api-key"
        mock_settings.llm.base_url = "https://api.openai.com/v1"
        mock_settings.llm.temperature = 0.7

        with patch('sentra.core.llm_server.llm_factory.settings', mock_settings):
            llm = LLMFactory.create_llm()

            assert llm.model_name == "gpt-3.5-turbo"

    def test_llm_factory_respects_base_url_from_config(self):
        """Test that LLMFactory uses base_url from configuration."""
        custom_base_url = "https://custom.api.example.com/v1"
        mock_settings = Mock()
        mock_settings.llm.provider = "openai"
        mock_settings.llm.model_name = "gpt-4"
        mock_settings.llm.api_key = "test-api-key"
        mock_settings.llm.base_url = custom_base_url
        mock_settings.llm.temperature = 0.7

        with patch('sentra.core.llm_server.llm_factory.settings', mock_settings):
            llm = LLMFactory.create_llm()

            assert llm.openai_api_base == custom_base_url


if __name__ == "__main__":
    pytest.main([__file__, "-v"])
