# Evidence

이 디렉토리는 Learning Phase에서 다음 단계로 넘어가기 위해 남기는 원본 증거와 요약 자료를 보관한다. 해석과 결론은 `docs/phases/<phase>/report.md`에 두고, 이곳에는 측정 조건과 원본 파일을 재확인할 수 있는 자료를 둔다.

## 구조

| 경로 | 역할 |
|---|---|
| `phase-XX/` | 특정 Learning Phase의 증거 묶음 |
| `phase-XX/README.md` | 해당 Phase evidence 인덱스 |
| `phase-XX/BASELINE.md` | 기준선 자체가 산출물인 Phase의 측정 요약 |
| `phase-XX/<scenario>/<condition>/` | k6 결과, SQL snapshot, Prometheus export 같은 원본 파일 |
| `phase-XX/grafana-screenshots/` | Grafana screenshot 모음 |

## 파일 역할

| 파일 | 내용 |
|---|---|
| `k6-summary.txt` | k6 실행 결과 요약 |
| `pg-stat-statements.txt` | 테스트 직후 `pg_stat_statements` snapshot |
| `reset.txt` | 통계 초기화나 실행 전 준비 기록 |
| `*-prometheus-*.json` | Prometheus query export |
| `hikari-*.json` | HikariCP 관련 Prometheus export |
| `*-seq-scan.json` | table scan이나 실행계획 확인용 export |
| `*.png` | Grafana dashboard screenshot |

## 명명 규칙

- Phase 디렉토리는 `phase-01`, `phase-02`처럼 zero-padding을 사용한다.
- 시나리오 디렉토리는 k6 scenario 이름과 맞춘다. 예: `orders`, `products`, `points`.
- 조건 디렉토리는 pool과 preset을 함께 적는다. 예: `pool10-baseline`, `pool10-page500`.
- screenshot 파일명은 시나리오와 조건을 드러내게 적는다. 예: `orders-baseline.png`.

## 문서 분리

- 실행 절차는 `docs/phases/<phase>/runbook.md`에 둔다.
- 관측 기준은 `docs/phases/<phase>/observability.md`에 둔다.
- 결과 해석과 다음 Phase 판단은 `docs/phases/<phase>/report.md`에 둔다.
- 원본 측정 자료와 기준선 요약은 `docs/evidence/<phase>/`에 둔다.

## 현재 Phase

- [Phase 1 Evidence](./phase-01/README.md)
