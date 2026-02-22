#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
API_KEY_HEADER="${API_KEY_HEADER:-X-API-Key}"
API_KEY="${API_KEY:-dev-key-1}"

PDF_PATH="${PDF_PATH:-data/sample.pdf}"
QUESTION="${QUESTION:-What is this document about?}"

POLL_TIMEOUT_SEC="${POLL_TIMEOUT_SEC:-90}"
POLL_INTERVAL_SEC="${POLL_INTERVAL_SEC:-1}"

fail() { echo "ERROR: $1" >&2; exit 1; }
need_cmd() { command -v "$1" >/dev/null 2>&1 || fail "Missing command: $1"; }

parse_body() { echo "$1" | sed '$d'; }
parse_code() { echo "$1" | tail -n 1; }

http_get() {
  local url="$1"
  curl -sS -w "\n%{http_code}" \
    -H "${API_KEY_HEADER}: ${API_KEY}" \
    "${url}"
}

http_post_file() {
  local url="$1"
  local file_path="$2"
  curl -sS -w "\n%{http_code}" \
    -H "${API_KEY_HEADER}: ${API_KEY}" \
    -F "file=@${file_path}" \
    "${url}"
}

http_post_json() {
  local url="$1"
  local json="$2"
  curl -sS -w "\n%{http_code}" \
    -H "${API_KEY_HEADER}: ${API_KEY}" \
    -H "Content-Type: application/json" \
    -d "${json}" \
    "${url}"
}

is_json_str() {
  local s="$1"
  python3 -c 'import sys,json; json.loads(sys.stdin.read())' <<< "$s" >/dev/null 2>&1
}

need_cmd curl
need_cmd python3

echo "0) Health check"
health_resp="$(http_get "${BASE_URL}/api/v1/health")"
health_body="$(parse_body "$health_resp")"
health_code="$(parse_code "$health_resp")"
if [[ "$health_code" != "200" ]]; then
  health_resp="$(http_get "${BASE_URL}/health")"
  health_body="$(parse_body "$health_resp")"
  health_code="$(parse_code "$health_resp")"
fi
[[ "$health_code" == "200" ]] || { echo "$health_body"; fail "Health check failed (HTTP ${health_code})"; }
echo "healthy_ok=true"

echo "1) Upload PDF -> get taskId"
[[ -f "${PDF_PATH}" ]] || fail "PDF not found: ${PDF_PATH}"

upload_resp="$(http_post_file "${BASE_URL}/documents" "${PDF_PATH}")"
upload_body="$(parse_body "$upload_resp")"
upload_code="$(parse_code "$upload_resp")"

if [[ "$upload_code" != "200" && "$upload_code" != "201" ]]; then
  echo "$upload_body"
  fail "Upload failed (HTTP ${upload_code})"
fi

task_id="$(python3 -c 'import sys,json; o=json.loads(sys.stdin.read()); print(o.get("taskId") or o.get("task_id") or o.get("id") or "")' <<< "$upload_body")"
[[ -n "$task_id" ]] || { echo "$upload_body"; fail "Missing taskId/task_id in response"; }
echo "task_id=${task_id}"

echo "2) Poll task status -> SUCCEEDED"
deadline=$(( $(date +%s) + POLL_TIMEOUT_SEC ))
status=""
doc_id=""
last_body=""

while true; do
  if (( $(date +%s) > deadline )); then
    echo "Last task response:"
    echo "${last_body}"
    fail "Task did not reach SUCCEEDED within ${POLL_TIMEOUT_SEC}s"
  fi

  task_resp="$(http_get "${BASE_URL}/api/v1/tasks/${task_id}")"
  task_body="$(parse_body "$task_resp")"
  task_code="$(parse_code "$task_resp")"

  if [[ "$task_code" == "404" ]]; then
    task_resp="$(http_get "${BASE_URL}/tasks/${task_id}")"
    task_body="$(parse_body "$task_resp")"
    task_code="$(parse_code "$task_resp")"
  fi

  last_body="$task_body"

  if [[ "$task_code" != "200" ]]; then
    echo "$task_body"
    fail "Task status failed (HTTP ${task_code})"
  fi

  status="$(python3 -c 'import sys,json; o=json.loads(sys.stdin.read()); print(o.get("status") or o.get("state") or "")' <<< "$task_body")"
  doc_id="$(python3 -c 'import sys,json; o=json.loads(sys.stdin.read()); print(o.get("docId") or o.get("doc_id") or o.get("documentId") or o.get("document_id") or "")' <<< "$task_body")"

  echo "status=${status} doc_id=${doc_id}"

  if [[ "$status" == "SUCCEEDED" ]]; then break; fi
  if [[ "$status" == "FAILED" ]]; then echo "$task_body"; fail "Task FAILED"; fi

  sleep "${POLL_INTERVAL_SEC}"
done

[[ -n "$doc_id" ]] || fail "Missing docId/doc_id after success"

echo "3) Query -> expect JSON answer + citations"

query_payload="$(python3 -c 'import json,os; print(json.dumps({"question": os.environ.get("QUESTION","What is this document about?"), "docId": os.environ.get("DOC_ID","")}))' DOC_ID="$doc_id")"

query_resp="$(http_post_json "${BASE_URL}/query" "$query_payload")"
query_body="$(parse_body "$query_resp")"
query_code="$(parse_code "$query_resp")"

if [[ "$query_code" == "404" ]]; then
  query_resp="$(http_post_json "${BASE_URL}/api/v1/query" "$query_payload")"
  query_body="$(parse_body "$query_resp")"
  query_code="$(parse_code "$query_resp")"
fi

if [[ "$query_code" != "200" ]]; then
  echo "$query_body"
  fail "Query failed (HTTP ${query_code})"
fi

if ! is_json_str "$query_body"; then
  echo "Query response is not valid JSON."
  echo "Body (first 400 chars):"
  echo "${query_body}" | head -c 400
  echo
  fail "Query returned non-JSON body"
fi

python3 -c 'import sys,json; r=json.loads(sys.stdin.read()); print("answer_ok=", bool(r.get("answer"))); print("citations=", len(r.get("citations", [])))' <<< "$query_body"

echo "SMOKE OK "