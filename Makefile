.PHONY: up down nuke logs smoke test-python wait-health verify

# Configuration with default values
BASE_URL ?= http://127.0.0.1:8080
API_KEY_HEADER ?= X-API-Key
API_KEY ?= dev-key-1

up:
	docker compose -f infra/docker-compose.yml up --build -d

down:
	docker compose -f infra/docker-compose.yml down

nuke:
	docker compose -f infra/docker-compose.yml down -v

logs:
	docker compose -f infra/docker-compose.yml logs -f --tail=200

smoke:
	./scripts/smoke_test.sh

test-python:
	pytest -q

wait-health:
	@echo "Waiting for backend health..."
	@for i in $$(seq 1 90); do \
		( curl -fsS -H "$(API_KEY_HEADER): $(API_KEY)" "$(BASE_URL)/api/v1/health" >/dev/null 2>&1 || \
		  curl -fsS -H "$(API_KEY_HEADER): $(API_KEY)" "$(BASE_URL)/health" >/dev/null 2>&1 ) && \
		echo "Healthy" && exit 0; \
		sleep 1; \
	done; \
	echo "ERROR: backend not healthy in time" && exit 1

verify: up wait-health smoke
	@echo "VERIFY OK"