from __future__ import annotations

import time
from dataclasses import dataclass
from typing import Dict


@dataclass
class Timings:
    stages_ms: Dict[str, float]

    def to_dict(self) -> dict:
        return dict(self.stages_ms)


class Timer:
    def __init__(self):
        self._t0 = time.perf_counter()

    def ms(self) -> float:
        return (time.perf_counter() - self._t0) * 1000.0
