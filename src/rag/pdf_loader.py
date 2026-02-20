from __future__ import annotations

from dataclasses import dataclass
from typing import List

from pypdf import PdfReader

from .errors import PDFParseError


@dataclass(frozen=True)
class PageText:
    page: int
    text: str


def load_pdf_pages(pdf_bytes: bytes, filename: str | None = None) -> List[PageText]:
    try:
        reader = PdfReader(io_bytes(pdf_bytes))
        pages: List[PageText] = []
        for i, page in enumerate(reader.pages):
            text = page.extract_text() or ""
            pages.append(PageText(page=i + 1, text=text))
        if not pages:
            raise PDFParseError("No pages extracted", filename=filename)
        return pages
    except PDFParseError:
        raise
    except Exception as e:
        raise PDFParseError(str(e), filename=filename)


def io_bytes(data: bytes):
    import io

    return io.BytesIO(data)
