#!/usr/bin/env bash
# 로컬 테스트용 Access Token 발급
#
# 인증 서버를 띄우지 않고도 로그인이 필요한 API를 호출해 볼 수 있게,
# 인증 서버와 같은 클레임 구조(userId, studentType, issuer)로 토큰을 만든다.
# .env의 JWT_SECRET으로 서명하므로 로컬 서버가 그대로 검증할 수 있다.
#
# 사용법:
#   ./scripts/issue-test-token.sh                      # userId=1, UNDERGRADUATE, 1시간
#   ./scripts/issue-test-token.sh 42 ON_LEAVE
#   ./scripts/issue-test-token.sh 42 GRADUATE 600      # 10분짜리
#
# 운영 시크릿으로는 절대 쓰지 않는다 (아무 사용자로나 토큰을 만들 수 있게 되므로).
set -euo pipefail

USER_ID="${1:-1}"
STUDENT_TYPE="${2:-UNDERGRADUATE}"
TTL_SECONDS="${3:-3600}"

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
if [[ -z "${JWT_SECRET:-}" && -f "$ROOT/.env" ]]; then
  JWT_SECRET="$(grep -E '^JWT_SECRET=' "$ROOT/.env" | tail -1 | cut -d= -f2-)"
  JWT_ISSUER="${JWT_ISSUER:-$(grep -E '^JWT_ISSUER=' "$ROOT/.env" | tail -1 | cut -d= -f2-)}"
fi
: "${JWT_SECRET:?JWT_SECRET이 없습니다. .env를 만들거나 환경변수로 넘겨 주세요}"
JWT_ISSUER="${JWT_ISSUER:-auth-backend}"

# Base64URL: 일반 Base64에서 +/ 를 -_ 로 바꾸고 끝의 = 를 뗀다
b64url() { openssl base64 -A | tr '+/' '-_' | tr -d '='; }

NOW=$(date +%s)
HEADER='{"alg":"HS256","typ":"JWT"}'
PAYLOAD=$(printf '{"sub":"%s","userId":%s,"role":"ROLE_USER","studentType":"%s","iss":"%s","iat":%s,"exp":%s}' \
  "$USER_ID" "$USER_ID" "$STUDENT_TYPE" "$JWT_ISSUER" "$NOW" "$((NOW + TTL_SECONDS))")

UNSIGNED="$(printf '%s' "$HEADER" | b64url).$(printf '%s' "$PAYLOAD" | b64url)"
SIGNATURE=$(printf '%s' "$UNSIGNED" | openssl dgst -sha256 -hmac "$JWT_SECRET" -binary | b64url)

echo "$UNSIGNED.$SIGNATURE"
