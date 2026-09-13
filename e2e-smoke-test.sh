#!/usr/bin/env bash
# End-to-end smoke test against a running `docker-compose up` stack.
# Exercises the golden path: register -> login -> create group -> create
# expense, failing loudly (with the offending response body) on the first
# unexpected status code instead of limping into the next step.
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"

for bin in curl jq; do
    if ! command -v "$bin" >/dev/null 2>&1; then
        echo "ERROR: '$bin' is required but not installed." >&2
        exit 1
    fi
done

RUN_ID="$(date +%s)-$RANDOM"
EMAIL="smoketest-${RUN_ID}@example.com"
PASSWORD="smoke-test-password-123"
DISPLAY_NAME="Smoke Test User"

# Performs one HTTP call and aborts the script with a descriptive message if
# the response status doesn't match. Status messages go to stderr and the
# response body goes to stdout, so callers can do `x=$(request ...)` to
# capture just the body without losing the progress output on screen.
request() {
    local step="$1" method="$2" path="$3" expected_status="$4" data="${5:-}" token="${6:-}"
    local url="${BASE_URL}${path}"
    local -a curl_args=(-s -o /tmp/e2e-smoke-body.$$ -w "%{http_code}" -X "$method" "$url" -H "Content-Type: application/json")
    [[ -n "$token" ]] && curl_args+=(-H "Authorization: Bearer $token")
    [[ -n "$data" ]] && curl_args+=(-d "$data")

    local status body
    status="$(curl "${curl_args[@]}")"
    body="$(cat /tmp/e2e-smoke-body.$$)"
    rm -f /tmp/e2e-smoke-body.$$

    if [[ "$status" != "$expected_status" ]]; then
        echo "FAILED: $step" >&2
        echo "  $method $url -> expected $expected_status, got $status" >&2
        echo "  response body: $body" >&2
        exit 1
    fi

    echo "OK: $step (HTTP $status)" >&2
    echo "$body"
}

echo "== OrtakPay e2e smoke test against $BASE_URL =="

register_body="$(request "register" POST /api/auth/register 201 \
    "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\",\"displayName\":\"$DISPLAY_NAME\"}")"
USER_ID="$(echo "$register_body" | jq -r '.id')"

login_body="$(request "login" POST /api/auth/login 200 \
    "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}")"
TOKEN="$(echo "$login_body" | jq -r '.token')"

group_body="$(request "create group" POST /api/groups 201 \
    "{\"name\":\"Smoke Test Trip\"}" "$TOKEN")"
GROUP_ID="$(echo "$group_body" | jq -r '.id')"

expense_payload=$(cat <<EOF
{
  "paidBy": "$USER_ID",
  "amount": 42.00,
  "description": "Smoke test dinner",
  "splitType": "EQUAL",
  "participants": [{"userId": "$USER_ID"}]
}
EOF
)
request "create expense" POST "/api/groups/${GROUP_ID}/expenses" 201 "$expense_payload" "$TOKEN" >/dev/null

echo "== All steps passed =="
