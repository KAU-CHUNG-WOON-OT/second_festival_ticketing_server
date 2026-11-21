# second_festival_ticketing_server

2학기 축제 티켓팅 서버. Spring Boot와 EC2에 직접 구성한 Redis로 선착순 발급을 처리한다.

## 구조

```
ALB (HTTPS) → Spring Boot api (EC2 × 2) → Redis (EC2, Primary + Replica)
                                             │ Stream
                                             ▼
                                   archiver (Spring Boot) → RDS MySQL
```

- **api**: JWT 검증, 신청 시간대 검증, Redis Lua 스크립트로 중복 확인·잔여 수량 확인·발급을 원자적으로 처리하고 결과를 즉시 응답
- **archiver**: Redis Stream의 발급 내역을 소비해 MySQL에 영구 저장

자세한 설계는 [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) 참고.

## 기술 스택

- Java 21, Spring Boot 4, Gradle (멀티 모듈: `api`, `archiver`)
- Redis 7 (Lua, Streams), MySQL 8
- Terraform, Docker, Testcontainers, k6

## 로컬 실행

```bash
cp .env.example .env          # 비밀번호 등 값 채우기
docker compose up -d          # redis, mysql 실행
./gradlew :api:bootRun
./gradlew :archiver:bootRun
```

## API 확인

- **Swagger UI**: 서버 실행 후 http://localhost:8080/swagger-ui.html (`prod` 프로필에서는 꺼짐)
- **테스트 토큰**: 인증 서버 없이 `.env`의 `JWT_SECRET`으로 Access Token을 만든다

  ```bash
  ./scripts/issue-test-token.sh                 # userId=1, UNDERGRADUATE
  ./scripts/issue-test-token.sh 42 ON_LEAVE 600 # userId, 학생 유형, 유효 시간(초)
  ```

  Swagger의 **Authorize** 버튼에 붙여넣으면 자물쇠가 있는 API를 호출할 수 있다.
- **요청 파일**: `http/tickets.http` (IntelliJ HTTP Client). 토큰은 `http/http-client.private.env.json`에 넣는다 (예시 파일 복사, 커밋 제외)
- **명세**: [docs/API.md](docs/API.md)

## 테스트

Testcontainers를 사용하므로 Docker가 실행 중이어야 한다.

```bash
./gradlew test
```
