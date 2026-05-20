# Phase 3 실행 절차

## 전체 흐름

```text
1. Phase 3 코드와 문서 준비
2. strategy별 단일 요청 SQL count 확인
3. strategy별 smoke k6 실행
4. strategy별 baseline k6 실행
5. pg_stat_statements, EXPLAIN, Grafana evidence 저장
6. report.md 작성
```

## 기본 실행 형식

```bash
PHASE=phase-03 STRATEGY=lazy ./k6/run.sh orders smoke prometheus
PHASE=phase-03 STRATEGY=fetch-join ./k6/run.sh orders smoke prometheus
PHASE=phase-03 STRATEGY=batch-size ./k6/run.sh orders smoke prometheus
PHASE=phase-03 STRATEGY=entity-graph ./k6/run.sh orders smoke prometheus
```
