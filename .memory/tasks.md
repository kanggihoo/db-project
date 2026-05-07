# Tasks

## 다음 작업 큐

- [ ] Phase 1 재현환경 준비: `docker-compose up -d`, 시더 실행, `VACUUM ANALYZE`, 데이터 건수 확인
- [ ] Phase 1 부하테스트 실행: 앱 실행 후 k6 3개 스크립트 실행
- [ ] `docs/evidence/phase1/`에 증빙 4장 저장: `01_k6_summary.png`, `02_grafana_p95.png`, `03_pg_stat_statements.png`, `04_hikari_active.png`
- [ ] `BASELINE.md` 생성 또는 갱신: `/api/orders`, `/api/products`, `/api/points`의 p50, p95, TPS, 에러율, DB 지표 기록
- [ ] Phase 2 착수 전 `RoadMap.md` 기준으로 인덱스 실험 계획을 별도 문서화

## 완료된 작업

- [x] Phase 0 기반 구현: 인프라, DDL, 시더, 테스트 인프라
- [x] Phase 1 나이브 API 구현
- [x] Phase 1 Repository 테스트 작성 및 GREEN 확인 문서화
- [x] Phase 1 k6 스크립트 3종 작성

## 보류 또는 확인 필요

- [ ] 실제 대량 데이터 목표 건수로 시딩할지, 현재 소량 테스트 모드로 측정할지 결정
- [ ] Grafana 대시보드 패널을 수동 구성할지, 최소 스크린샷만으로 Phase 1 증빙을 남길지 결정
