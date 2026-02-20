#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
API_KEY_HEADER="${API_KEY_HEADER:-X-API-Key}"
API_KEY="${API_KEY:-dev-key-1}"
PDF_PATH="${PDF_PATH:-data/sample.pdf}"

fail() {
  echo "ERROR: $1" >&2
  exit 1
}

http_post_file() {
  local url="$1"
  local file_path="$2"
  local out
  out="$(curl -sS -w "\n%{http_code}" \
    -H "${API_KEY_HEADER}: ${API_KEY}" \
    -F "file=@${file_path}" \
    "${url}")"
  echo "$out"
}

http_get() {
  local url="$1"
  local out
  out="$(curl -sS -w "\n%{http_code}" \
    -H "${API_KEY_HEADER}: ${API_KEY}" \
    "${url}")"
  echo "$out"
}

http_post_json() {
  local url="$1"
  local json="$2"
  local out
  out="$(curl -sS -w "\n%{http_code}" \
    -H "${API_KEY_HEADER}: ${API_KEY}" \
    -H "Content-Type: application/json" \
    -d "${json}" \
    "${url}")"
  echo "$out"
}

parse_body() {
  echo "$1" | sed '$d'
}

parse_code() {
  echo "$1" | tail -n 1
}

echo "1) Upload PDF -> get taskId"
[[ -f "${PDF_PATH}" ]] || fail "PDF not found: ${PDF_PATH}"

resp="$(http_post_file "${BASE_URL}/documents" "${PDF_PATH}")"
body="$(parse_body "$resp")"
code="$(parse_code "$resp")"

if [[ "$code" != "200" && "$code" != "201" ]]; then
  echo "$body"
  fail "Upload failed (HTTP ${code})"
fi

task_id="$(python3 -c 'import sys,json; print(json.loads(sys.stdin.read()).get("task_id",""))' <<< "$body")"
[[ -n "$task_id" ]] || { echo "$body"; fail "Missing task_id in response"; }
echo "task_id=${task_id}"

echo "2) Poll task status -> SUCCEEDED"
status=""
doc_id=""
for i in $(seq 1 60); do
  resp="$(http_get "${BASE_URL}/tasks/${task_id}")"
  body="$(parse_body "$resp")"
  code="$(parse_code "$resp")"
  if [[ "$code" != "200" ]]; then
    echo "$body"
    fail "Task status failed (HTTP ${code})"
  fi

  status="$(python3 -c 'import sys,json; print(json.loads(sys.stdin.read()).get("status",""))' <<< "$body")"
  doc_id="$(python3 -c 'import sys,json; print(json.loads(sys.stdin.read()).get("doc_id",""))' <<< "$body")"
  echo "status=${status} doc_id=${doc_id}"
  if [[ "$status" == "SUCCEEDED" ]]; then
    break
  fi
  if [[ "$status" == "FAILED" ]]; then
    echo "$body"
    fail "Task FAILED"
  fi
  sleep 1
done

[[ "$status" == "SUCCEEDED" ]] || fail "Task did not reach SUCCEEDED in time"
[[ -n "$doc_id" ]] || fail "Missing doc_id after success"

echo "3) Query -> expect answer + citations"
q='{"question":"What is this document about?"}'
resp="$(http_post_json "${BASE_URL}/query" "$q")"
body="$(parse_body "$resp")"
code="$(parse_code "$resp")"

if [[ "$code" != "200" ]]; then
  echo "$body"
  fail "Query failed (HTTP ${code})"
fi

echo "$body" | python3 -c 'import sys,json; r=json.loads(sys.stdin.read()); print("answer_ok=", bool(r.get("answer"))); print("citations=", len(r.get("citations",[])))'
echo "SMOKE OK"
