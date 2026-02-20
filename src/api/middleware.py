from __future__ import annotations

import time
import uuid
from typing import Callable

from fastapi import Request, Response


REQUEST_ID_HEADER = "X-Request-ID"


async def request_id_middleware(request: Request, call_next: Callable):
    req_id = request.headers.get(REQUEST_ID_HEADER) or uuid.uuid4().hex
    request.state.request_id = req_id

    t0 = time.perf_counter()
    response: Response = await call_next(request)
    dt_ms = (time.perf_counter() - t0) * 1000.0

    response.headers[REQUEST_ID_HEADER] = req_id
    response.headers["X-Latency-Ms"] = f"{dt_ms:.2f}"
    return response
