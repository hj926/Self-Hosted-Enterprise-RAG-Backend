from __future__ import annotations

import argparse

from .config import RagSettings
from .rag_engine import RagEngine


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--q", required=True)
    p.add_argument("--doc_id", default=None)
    args = p.parse_args()

    settings = RagSettings()
    engine = RagEngine(settings)

    res = engine.query(question=args.q, doc_id=args.doc_id)
    print(res.answer)
    for c in res.citations:
        print({"doc_id": c.doc_id, "page": c.page, "snippet": c.snippet[:120]})


if __name__ == "__main__":
    main()
