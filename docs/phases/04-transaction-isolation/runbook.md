# Phase 4 실행 절차

## 전체 흐름

```text
1. Phase 4 코드와 SQL helper 준비
2. READ COMMITTED / REPEATABLE READ 읽기 이상 현상 재현
3. SERIALIZABLE 동시 갱신 실패 재현
4. k6, pg_stat_statements, Grafana evidence 저장
5. report.md 작성
```

## 준비

Phase 4는 Phase 3의 orders loading strategy 설정을 사용하지 않는다. 공통 k6 `baseline` preset은 50 rps 기준선이고, Phase 4 실험에는 별도 scenario/preset을 추가해서 사용한다.

```bash
docker compose up -d
./scripts/server.sh pool10
```

## Evidence 위치

```text
docs/evidence/phase-04/
├── isolation-read-anomalies/
├── serializable-conflicts/
└── grafana-screenshots/
```

## 예정된 반복 순서

각 실험은 같은 데이터 상태에서 실행한다.

```text
1. DB 통계 초기화
2. 격리 수준별 SQL 또는 API 실행
3. pg_stat_statements 저장
4. k6 summary 저장
5. Grafana screenshot 저장
6. report.md에 결과 반영
```

구체적인 명령은 Phase 4 구현 계획에서 SQL helper와 k6 scenario를 추가한 뒤 확정한다.
