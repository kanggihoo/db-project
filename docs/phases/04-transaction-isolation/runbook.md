# Phase 4 실행 절차

## 전체 흐름

```text
1. Phase 4 코드와 SQL helper 준비
2. Dirty Read 방지 확인
3. READ COMMITTED / REPEATABLE READ 읽기 이상 현상 재현
4. Lost Update 동시 갱신 충돌 재현
5. SQL transcript, integration test output, server log evidence 저장
5. report.md 작성
```

## 준비

Phase 4는 Phase 3의 orders loading strategy 설정을 사용하지 않는다. 이 Phase의 핵심은 대량 부하가 아니라 트랜잭션 실행 순서이므로, 두 connection 또는 두 thread를 사용해 순서를 고정한 재현 테스트를 우선한다. k6는 반복 부하 지표가 필요할 때만 선택적으로 사용한다.

```bash
docker compose up -d
./scripts/server.sh pool10
```

## Evidence 위치

```text
docs/evidence/phase-04/
├── dirty-read/
├── non-repeatable-read/
├── phantom-read/
├── lost-update/
└── optional-load/
```

## 예정된 반복 순서

각 실험은 필요한 row만 known state로 되돌린 뒤 실행한다. 전체 DB를 매번 초기화하지 않는다.

```text
1. 대상 Product 또는 Product SKU row를 known state로 reset
2. 격리 수준별 SQL 또는 integration test 실행
3. transaction transcript 또는 test output 저장
4. concurrent update failure가 있으면 server log excerpt 저장
5. 필요 시 pg_stat_statements snapshot 저장
6. report.md에 결과 반영
```

구체적인 명령은 Phase 4 구현 계획에서 SQL helper와 thread 기반 integration test를 추가한 뒤 확정한다.
