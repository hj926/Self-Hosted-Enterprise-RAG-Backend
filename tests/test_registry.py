from __future__ import annotations

from src.rag.registry import DocumentRegistry


def test_registry_upsert_and_get(tmp_path):
    reg_path = tmp_path / "registry.json"
    reg = DocumentRegistry(str(reg_path))

    rec = reg.upsert("doc1", "a.pdf", ["c1", "c2"])
    got = reg.get("doc1")

    assert got.doc_id == "doc1"
    assert got.filename == "a.pdf"
    assert got.chunk_ids == ["c1", "c2"]
    assert rec.uploaded_at is not None


def test_registry_delete(tmp_path):
    reg_path = tmp_path / "registry.json"
    reg = DocumentRegistry(str(reg_path))

    reg.upsert("doc1", "a.pdf", ["c1"])
    deleted = reg.delete("doc1")
    assert deleted.doc_id == "doc1"

    listed = reg.list()
    assert len(listed) == 0
