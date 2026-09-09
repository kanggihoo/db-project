# 앞으로 강제할 Rule

1. 모든 Phase는 observability.md에 Measurement Contract를 먼저 정의한다.
2. Prometheus label은 phase, scenario, preset, pool만 허용한다. userId, page, SQL text는 metadata/preset에 둔다.

3. 테스트에 사용된 db의 상태 초기화 하는 로직

- 테이블별 데이터 개수
- 처음 인덱스 초기화 (다른 phase에서 추가된 인덱스나 데이터를 맨 처음 db 상태랑 동일하게 셋팅 후 Voccum으로 진행 )

3. 각 scenario run 전 pg_stat_statements_reset()과 필요한 VACUUM ANALYZE를 실행하고 기록한다.

4. report 문서 작성 지침과 관련된 rule
