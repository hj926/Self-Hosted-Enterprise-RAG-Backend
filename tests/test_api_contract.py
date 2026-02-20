from __future__ import annotations


def test_health_endpoint(client):
    r = client.get("/health")
    assert r.status_code in (200, 503, 504)
