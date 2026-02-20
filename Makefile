.PHONY: up down logs smoke test-python

up:
	docker compose -f infra/docker-compose.yml up --build -d

down:
	docker compose -f infra/docker-compose.yml down -v

logs:
	docker compose -f infra/docker-compose.yml logs -f --tail=200

smoke:
	./scripts/smoke_test.sh

test-python:
	pytest -q
