# Phase 7 관측 기준

## 주요 증거

| 증거 | 역할 |
|---|---|
| `EXPLAIN (ANALYZE, BUFFERS)` | Offset deep과 Cursor deep의 DB 실행계획 비교 |
| k6 summary | API p95 비교 |
| `pg_stat_statements` | data query와 count query 호출/시간 비교 |
| Grafana 스크린샷 | 같은 시간대의 HTTP, Hikari, table access 보조 확인 |

## Grafana 사용

별도 Phase 7 dashboard를 만들지 않고 shared `DB Lab Overview`를 사용한다.

캡처 대상:

- Run Summary 영역
- k6 Load 영역
- Hikari Pool 영역
- Table Access의 `point_history`

Grafana는 보조 증거다. Offset이 읽고 지나간 row 수 차이는 `EXPLAIN (ANALYZE, BUFFERS)`로 판단한다.

## k6 label 원칙

- `phase`: `phase-07`
- 기존 `scenario`: `points-offset`, `points-cursor`
- 재측정 `scenario`: `points-offset-sampling`, `points-cursor-sampling`
- 기존 `preset`: `page0`, `mid`, `deep`, `cursor`
- 재측정 `preset`: `offset-sampling`, `cursor-sampling`
- `pool`: `pool10`

`userId`, `page`, SQL text는 label로 넣지 않는다.

재측정 k6는 제한된 bucket만 사용한다. Offset은 `points_offset_page_*_duration`, Cursor는 `points_cursor_page_*_duration` Trend metric으로 page sample별 p95를 남긴다.

Cursor sampling은 iteration당 하나의 요청만 보낸다. Cursor source lookup은 사전 계산한 sample 파일에만 있고 API latency에는 포함하지 않는다.
