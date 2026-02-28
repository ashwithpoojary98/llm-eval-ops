"""
Tests for API endpoints.
"""
import pytest
from fastapi.testclient import TestClient


@pytest.fixture
def client():
    """Create test client."""
    from app.main import app
    return TestClient(app)


class TestHealthEndpoints:
    """Tests for health check endpoints."""

    def test_health_check(self, client):
        """Test main health endpoint."""
        response = client.get("/health")
        assert response.status_code == 200
        data = response.json()
        assert "status" in data
        assert "version" in data

    def test_liveness_probe(self, client):
        """Test liveness probe."""
        response = client.get("/health/live")
        assert response.status_code == 200
        assert response.json()["status"] == "alive"


class TestMetricsEndpoints:
    """Tests for metrics info endpoints."""

    def test_list_metrics(self, client):
        """Test listing all metrics."""
        response = client.get("/metrics")
        assert response.status_code == 200
        data = response.json()
        assert isinstance(data, list)
        assert len(data) > 0

        # Check metric structure
        metric = data[0]
        assert "code" in metric
        assert "name" in metric
        assert "category" in metric
        assert "requires_ground_truth" in metric

    def test_list_categories(self, client):
        """Test listing metric categories."""
        response = client.get("/metrics/categories")
        assert response.status_code == 200
        data = response.json()
        assert isinstance(data, list)

    def test_get_metric_info(self, client):
        """Test getting specific metric info."""
        response = client.get("/metrics/BLEU")
        assert response.status_code == 200
        data = response.json()
        assert data["code"] == "BLEU"
        assert data["requires_ground_truth"] is True


class TestRootEndpoint:
    """Tests for root endpoint."""

    def test_root(self, client):
        """Test root endpoint."""
        response = client.get("/")
        assert response.status_code == 200
        data = response.json()
        assert "name" in data
        assert "version" in data
        assert "docs" in data


if __name__ == "__main__":
    pytest.main([__file__, "-v"])
