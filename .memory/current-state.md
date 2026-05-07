# Current State

## 현재 단계

- Phase 0 구현 완료: Docker Compose, PostgreSQL 17, Prometheus, Grafana, postgres-exporter, 전체 DDL, 시더, Testcontainers 기반 테스트가 준비되어 있다.
- Phase 1 구현 완료: 나이브 API 3종, Repository/Service/Controller/DTO, Repository 테스트, k6 스크립트가 존재한다.
- Phase 1은 아직 측정 완료 상태가 아니다. 부하테스트 직전 재현환경 구성, 증빙 스크린샷 저장, `BASELINE.md` 정량 표 작성이 남아 있다.

## 바로 참고할 파일

- `.memory/README.md`: 메모리 시스템 운영 규칙
- `docs/PHASE1-OVERVIEW.md`: Phase 1 구현 완료/미완 항목 구분
- `RoadMap.md`: 전체 Phase 흐름과 다음 단계 기준
- `SETUP.md`: 인프라 실행, 시딩, 검증 방법
- `ERD_ANALYSIS.md`: 도메인별 DB 설계 의도

## 현재 구현 요약

- 애플리케이션 루트: `ecommerce/`
- Java 패키지: `com.dblab.ecommerce`
- Phase 1 API:
  - `GET /api/orders?userId=`: 주문 목록 조회, 서비스 루프에서 `findByOrderId`를 호출해 N+1을 의도적으로 유발
  - `GET /api/products?categoryId=&status=`: 인덱스 없는 카테고리/상태 필터로 Seq Scan 유도
  - `GET /api/points?userId=&page=&size=`: Spring Data JPA `PageRequest` 기반 Offset 페이징
- k6 스크립트:
  - `k6/orders-test.js`
  - `k6/products-test.js`
  - `k6/points-test.js`

## 주의할 점

- `docs/sql-quiz/`는 이 메모리 시스템과 무관하므로 읽지 않는다.
- Windows PowerShell 환경에서는 파일을 읽을 때 `-Encoding UTF8`을 명시해야 한글이 깨지지 않는다.
- 세션 시작 시 전체 source code를 탐색하지 않는다. 먼저 `.memory` 파일을 읽고, 필요한 경우에만 지정 파일을 확인한다.
- Phase 2로 넘어가기 전 Phase 1의 베이스라인 증빙을 먼저 채워야 한다.
