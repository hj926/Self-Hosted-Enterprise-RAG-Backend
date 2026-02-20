from __future__ import annotations

import argparse
from pathlib import Path

from .config import RagSettings
from .rag_engine import RagEngine


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--pdf", required=True)
    args = p.parse_args()

    settings = RagSettings()
    engine = RagEngine(settings)

    pdf_path = Path(args.pdf)
    data = pdf_path.read_bytes()
    rec = engine.ingest_pdf(data, filename=pdf_path.name)
    print(
        {
            "doc_id": rec.doc_id,
            "filename": rec.filename,
            "uploaded_at": rec.uploaded_at,
            "chunk_count": len(rec.chunk_ids),
        }
    )


if __name__ == "__main__":
    main()
