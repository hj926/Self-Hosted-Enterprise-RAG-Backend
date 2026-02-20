from __future__ import annotations

from typing import Any

import httpx

from .errors import OllamaUnavailableError, UpstreamTimeoutError


class OllamaClient:
    def __init__(self, base_url: str, timeout_seconds: int):
        self.base_url = base_url.rstrip("/")
        self.timeout = httpx.Timeout(timeout_seconds)

    def health_probe(self) -> None:
        try:
            with httpx.Client(timeout=self.timeout) as client:
                r = client.get(f"{self.base_url}/api/tags")
                r.raise_for_status()
        except httpx.TimeoutException as e:
            raise UpstreamTimeoutError(str(e))
        except Exception as e:
            raise OllamaUnavailableError(str(e))

    def embed(self, model: str, text: str) -> list[float]:
        payload = {"model": model, "input": text}

        try:
            with httpx.Client(timeout=self.timeout) as client:
                r = client.post(f"{self.base_url}/api/embed", json=payload)
                if r.status_code == 404:
                    r = client.post(f"{self.base_url}/api/embeddings", json=payload)

                r.raise_for_status()
                data: Any = r.json()

                emb: Any = None
                if isinstance(data, dict):
                    embs = data.get("embeddings")
                    if (
                        isinstance(embs, list)
                        and len(embs) > 0
                        and isinstance(embs[0], list)
                    ):
                        emb = embs[0]
                    else:
                        emb = data.get("embedding")

                if not isinstance(emb, list):
                    raise OllamaUnavailableError("Invalid embeddings response")

                return [float(x) for x in emb]

        except httpx.TimeoutException as e:
            raise UpstreamTimeoutError(str(e))
        except UpstreamTimeoutError:
            raise
        except Exception as e:
            raise OllamaUnavailableError(str(e))

    def generate(
        self, model: str, prompt: str, temperature: float, max_tokens: int
    ) -> str:
        payload = {
            "model": model,
            "prompt": prompt,
            "options": {
                "temperature": temperature,
                "num_predict": max_tokens,
            },
            "stream": False,
        }

        try:
            with httpx.Client(timeout=self.timeout) as client:
                r = client.post(f"{self.base_url}/api/generate", json=payload)
                r.raise_for_status()
                data: Any = r.json()
                if isinstance(data, dict):
                    return str(data.get("response", ""))
                return ""
        except httpx.TimeoutException as e:
            raise UpstreamTimeoutError(str(e))
        except Exception as e:
            raise OllamaUnavailableError(str(e))
