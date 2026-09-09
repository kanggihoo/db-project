# Ecommerce DB Optimization Lab

최적화하지 않은 나이브한 구현에서 시작해, DB 성능 문제를 **재현 → 측정 → 개선 → 재측정**하고 그 근거를 문서로 남기는 학습 프로젝트입니다.

목표는 "개선했다"가 아니라 **k6, Grafana, `EXPLAIN ANALYZE`, `pg_stat_statements`로 Before/After를 정량적으로 증명하는 것**입니다. 모든 Phase는 측정 조건과 원본 산출물이 `docs/evidence/`에 남아야 완료로 봅니다.

## 진행 상황

**Phase 0 ~ 7 완료** / Phase 8 ~ 13 진행 예정

| Phase | 주제 | 결과 보고서 |
|---|---|---|
| 0 | 프로젝트 셋업 (Docker Compose, 스키마, 시딩) | [report](docs/phases/00-setup/report.md) |
| 1 | 베이스라인 확보 | [report](docs/phases/01-baseline/report.md) |
| 2 | 인덱스 설계 + 실행계획 분석 | [report](docs/phases/02-indexes/report.md) |
| 3 | N+1 + 로딩 전략 | [report](docs/phases/03-n-plus-one/report.md) |
| 4 | 트랜잭션 격리 수준 | [report](docs/phases/04-transaction-isolation/report.md) |
| 5 | 쿼리 최적화 + QueryDSL | [report](docs/phases/05-querydsl/report.md) |
| 6 | 집계 쿼리 최적화 | [report](docs/phases/06-aggregation/report.md) |
| 7 | 페이지네이션 (Offset vs Cursor) | [report](docs/phases/07-pagination/report.md) |
| 8 ~ 13 | DB Observability, Failure Injection, 스키마 마이그레이션, 동시성 제어, Outbox, CDC + Kafka | 예정 ([roadmap](RoadMap.md)) |

## 측정된 결과

### Phase 2 — 복합 인덱스

`product(category_id, status)` 조건이 `Seq Scan`으로 동작하던 상품 검색 API에 복합 인덱스를 적용했습니다. **API 코드는 변경하지 않았습니다.**

| 지표 | Before | After |
|---|---:|---:|
| k6 p95 | 621.91 ms | **18.49 ms** |
| k6 p99 | 2065.98 ms | **81.38 ms** |
| 상품 검색 SQL 평균 (`pg_stat_statements`) | 21.45 ms | **9.13 ms** |

실행계획은 `Seq Scan` → `Bitmap Index Scan + Bitmap Heap Scan`으로 전환됐습니다.
조건: `loadtest` seed, `pool10` 프로필, 50 rps, 5분.

### Phase 3 — N+1과 로딩 전략

`Orders → OrderItems → ProductSku → Product → ProductImages` 경로의 주문 목록 조회에서 로딩 전략별 요청당 SQL 수를 비교했습니다.

| 전략 | 평균 SQL calls/request |
|---|---:|
| Lazy (naive) | 약 2,454 |
| Fetch Join / EntityGraph | 약 698 ~ 699 |
| BatchSize | **약 27.2** |

Fetch Join과 EntityGraph는 `ProductImages` 반복 select가 남았고, 이번 측정에서는 BatchSize가 가장 낮은 p95를 기록했습니다. 결론은 "무조건 fetch join"이 아니라 페이징·collection 중복·row multiplication을 함께 보고 전략을 고르는 것입니다.

### Phase 6 — 집계 쿼리에서 인덱스가 항상 듣지는 않는다

상품 리뷰 요약은 `LIMIT 100`이 있어도 `ORDER BY AVG(rating)` 때문에 전체 후보를 먼저 집계해야 했고, `review(product_id)` / `review(product_id, rating)` 인덱스를 만들어도 `Seq Scan + HashAggregate + Sort` 계획이 유지됐습니다. 반면 월별 주문 집계는 `DATE_TRUNC('month', created_at)` 표현식에 맞춘 조건에서 외부 merge sort와 temp I/O가 사라졌습니다.

다만 실제 계획은 `Index Scan`이 아니라 `Parallel Seq Scan + Partial HashAggregate + Finalize GroupAggregate`였으므로, "표현식 인덱스를 타서 빨라졌다"고 단정하지 않았습니다.

### Phase 7 — Offset vs Cursor

`point_history` 10만 건 hot user 조건에서 둘을 속도 경쟁이 아니라 trade-off로 분리해 측정했습니다. Cursor는 페이지 번호 점프를 대체하지 않으므로, cursor 값을 사전 계산해 cursor-source `OFFSET` lookup 비용을 측정에서 제외했습니다. Offset/Page에서는 count query가 발생했고 Cursor에서는 발생하지 않았습니다.

## 기술 스택

| 구분 | 기술 |
|---|---|
| Language / Framework | Java 21, Spring Boot 4.x |
| ORM / Query | Spring Data JPA, QueryDSL |
| Database | PostgreSQL 17-alpine |
| Monitoring | Prometheus, Grafana, Micrometer |
| Load Test | k6 |
| Test | JUnit, Testcontainers, AssertJ |
| Seed Data | DataFaker, 100만 건 이상 |

## 문서 구조

| Path | 역할 |
|---|---|
| [`RoadMap.md`](RoadMap.md) | 전체 로드맵 인덱스 |
| [`docs/roadmap/`](docs/roadmap) | Phase별 목표와 완료 조건 |
| [`docs/phases/`](docs/phases) | Phase별 scope, runbook, observability, report |
| [`docs/evidence/`](docs/evidence) | k6 summary, Grafana 캡처, SQL 분석 원본 |
| [`docs/guides/`](docs/guides) | 환경, 시딩, Spring profile, k6, Grafana 공통 가이드 |
| [`docs/adr/`](docs/adr) | 의사결정 기록 |

## 실행

[`SETUP.md`](SETUP.md)와 [`docs/guides/environment.md`](docs/guides/environment.md)를 참고하세요.

## 원칙

- 각 Phase는 대표 테이블 하나로 범위를 좁혀 실험한다.
- 측정 결과는 문서 본문이 아니라 `docs/evidence/` 아래에 원본으로 저장한다.
- 측정 조건(seed, 프로필, rps, duration, 인덱스 상태)을 명시하지 않은 수치는 결론으로 쓰지 않는다.
- 계획 노드 이름만으로 결론 내리지 않는다. 버퍼, temp I/O, 정렬 방식, 병렬화 여부를 함께 본다.
- Phase 전환은 완료 조건과 증빙이 채워졌을 때만 한다.
