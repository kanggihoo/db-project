---
name: phase-evidence-portfolio-review
description: 현재 프로젝트의 Learning Phase 산출물을 검토해 목표 구현 여부, evidence 충분성, report.md 완결성, 다음 Phase 진행 가능성, Vercel/Docusaurus용 portfolio.mdx 작성 가능 여부를 판단한다. 사용자가 phase 리뷰, phase closeout, evidence 검토, report.md 품질 점검, 백엔드 포트폴리오 case study, Docusaurus/Vercel 배포용 MDX 생성을 요청할 때 사용한다.
---

# Phase Evidence Portfolio Review

## 목적

현재 프로젝트의 Learning Phase를 시니어 백엔드 엔지니어이자 포트폴리오 리뷰어 관점에서 검토한다.

확인할 사항:

- Phase 목표가 실제 코드, 테스트, 설정, 문서에 구현되었는가.
- 목표를 증명하는 evidence가 모두 수집되었고 재확인 가능한가.
- `report.md`가 계획, 구현, 측정 조건, 핵심 지표, 해석, 한계, 다음 Phase 판단을 독립적으로 설명하는가.
- 수집된 evidence와 `report.md`의 주장, 숫자, 결론이 일치하는가.
- SQL `EXPLAIN ANALYZE`, k6 결과, 애플리케이션 로그, Grafana 캡처, DB 지표에 이상 징후가 없는가.
- 다음 Phase로 넘어갈 준비가 되었는가.
- 공개용 `portfolio.mdx`를 바로 만들 수 있는가.

## 경로 해석

먼저 대상 Phase를 확정한다. 모든 Phase 디렉토리를 훑지 않는다.

사용자가 경로를 직접 주면 그 경로를 우선한다. Phase 이름만 주면 다음 규칙으로 해석한다.

- Phase 문서: `docs/phases/<phase-doc-dir>/`
- Phase 표준 파일: `README.md`, `scope.md`, `runbook.md`, `observability.md`, `report.md`
- Evidence: `docs/evidence/<evidence-dir>/`
- Roadmap: `docs/roadmap/<roadmap-file>.md`

현재 프로젝트에서는 같은 Phase라도 디렉토리 이름이 다를 수 있다.

- 예: Phase 7 문서 디렉토리 `docs/phases/07-pagination`
- 예: Phase 7 evidence 디렉토리 `docs/evidence/phase-07`
- 예: Phase 7 roadmap 파일 `docs/roadmap/08-phase-7-pagination.md`

따라서 같은 `<phase>` 문자열을 모든 경로에 그대로 대입하지 않는다.

권장 해석 순서:

1. `docs/phases/README.md`를 읽는다.
2. 대상 Phase 문서 디렉토리를 확정한다. 숫자 입력은 zero-padding된 `docs/phases/<NN>-*` 후보로 해석한다.
3. `docs/phases/<phase-doc-dir>/README.md`를 읽고 roadmap/evidence 링크를 따른다.
4. README에 링크가 없을 때만 `docs/roadmap/*phase-<N>*`와 `docs/evidence/phase-<NN>`을 후보로 삼는다.
5. 후보가 여러 개이거나 Phase가 불명확하면 사용자에게 묻는다.

## 검토 입력

기본적으로 아래 파일을 읽는다.

- `docs/phases/README.md`
- `docs/phases/<phase-doc-dir>/README.md`
- `docs/phases/<phase-doc-dir>/scope.md`
- `docs/phases/<phase-doc-dir>/runbook.md`
- `docs/phases/<phase-doc-dir>/observability.md`
- `docs/phases/<phase-doc-dir>/report.md`
- 대상 roadmap 파일
- `docs/evidence/README.md`
- `docs/evidence/<evidence-dir>/README.md`

필요할 때만 evidence 원자료를 읽는다. 먼저 evidence README와 `report.md`가 참조한 파일을 우선한다.

## 검토 절차

1. Phase 목표와 완료 조건을 추출한다.
   - roadmap, phase README, `scope.md`에서 목표, 제외 범위, 성공 기준, 대상 API/테이블, 기대 지표를 정리한다.
2. 구현 여부를 확인한다.
   - 목표별로 실제 코드, 테스트, 설정, 스크립트, 쿼리, 문서 변경이 있는지 매핑한다.
   - 파일명만 보고 판단하지 않는다. 필요한 파일을 열어 구현과 테스트 의도를 확인한다.
3. Evidence 수집 여부를 확인한다.
   - `docs/evidence/<evidence-dir>/README.md`가 주요 원자료를 인덱싱하는지 확인한다.
   - k6 summary, `pg_stat_statements`, `EXPLAIN ANALYZE`, Prometheus export, Grafana screenshot, integration test output, data profile 같은 원자료를 주장별로 연결한다.
4. `report.md`를 독립 문서로 평가한다.
   - README나 evidence를 보지 않아도 문제, 가설, 실험 조건, 데이터 규모, 지표, 결과, 해석, 한계, 다음 단계가 이해되는지 확인한다.
5. Claim -> Evidence 매트릭스를 만든다.
   - `report.md`의 핵심 주장마다 근거 파일, 지표, 실행 조건, 충분성 판단을 연결한다.
6. 지표와 원자료의 이상 징후를 확인한다.
   - 최종 숫자가 그럴듯해도 측정 조건, sample size, 실패율, p95/p99, 캐시, warm-up, 락, 커넥션 풀, DB 상태가 빠져 있으면 gap으로 본다.
7. 다음 Phase 준비도를 판단한다.
   - 완료 조건, 남은 리스크, 문서/evidence 보강 필요 여부를 기준으로 `ready`, `ready after fixes`, `not ready` 중 하나로 판단한다.
8. `portfolio.mdx` 준비도를 판단한다.
   - `report.md`가 충분하면 `references/portfolio-mdx.md`를 읽고 공개용 `portfolio.mdx`를 작성하거나 구조를 제안한다.
   - 사용자가 `profile.mdx`라고 말하면 공개용 포트폴리오 MDX인 `portfolio.mdx` 의도로 해석한다.

## 필수 점검

- 구현 매트릭스: Phase 목표마다 구현 파일, 테스트, 실행 명령, evidence를 연결한다.
- Claim -> Evidence 매트릭스: `report.md` 주요 주장마다 evidence 파일과 지표를 연결한다.
- `report.md` 독립성 테스트: 문제, 가설, 실험, 데이터, 결과, 해석, 한계, 다음 단계가 독립적으로 설명되는지 본다.
- 측정 방법론: workload, VU/duration, request mix, target endpoint, data size, sample size, 단위, 전후 비교, 실패율을 확인한다.
- SQL 신뢰성: `EXPLAIN ANALYZE`의 actual time, rows, loops, index 사용, seq scan, buffer hit/read, estimated/actual rows 차이를 확인한다.
- k6 신뢰성: `http_req_duration`, p95/p99, `http_req_failed`, `http_reqs`, iterations, thresholds, warm-up, duration이 결론과 맞는지 확인한다.
- 포트폴리오 서사: 문제 인식 -> 가설 -> 실험 설계 -> 측정 -> 분석 -> 의사결정 -> 한계 -> 다음 개선 흐름이 보이는지 확인한다.
- 면접 방어 가능성: 면접관이 물을 질문을 예상하고 현재 문서와 evidence만으로 답변 가능한지 평가한다.
- Docusaurus 전환 가능성: `portfolio.mdx`가 3-5분 안에 문제, 접근, 측정, 결과, 역량을 전달할 수 있는지 판단한다.

## portfolio.mdx 작성

`portfolio.mdx`를 만들거나 구체 구조를 제안해야 할 때만 `references/portfolio-mdx.md`를 읽는다.

작성 조건:

- `report.md`가 기술 보고서로 충분해야 한다.
- 핵심 지표와 evidence 링크가 방어 가능해야 한다.
- 공개용 문서에서 반복할 내용과 `report.md`로 보낼 상세 내용을 분리할 수 있어야 한다.

기본 출력 위치:

- `docs/phases/<phase-doc-dir>/portfolio.mdx`

필요하면 `assets/portfolio-template.mdx`를 복사해 대상 Phase 값으로 채운다. placeholder를 그대로 남기지 않는다.

원칙:

- `portfolio.mdx`는 `report.md`의 축약 복사본이 아니다.
- 상세 실험과 raw evidence는 `report.md`와 evidence 링크로 보낸다.
- 공개 문서에는 문제, 접근, 핵심 지표, 해석, 의사결정, 백엔드 역량 포인트를 남긴다.
- report가 불충분하면 `portfolio.mdx`를 억지로 만들지 말고 보강해야 할 `report.md` 섹션과 evidence를 먼저 제안한다.

## 출력 형식

사용자가 다른 언어를 요청하지 않는 한 한국어로 응답한다.

이 스킬 자체를 작성하거나 수정하는 동안에는 사용자가 최종 승인하기 전까지 `SKILL.md` 초안도 한국어로 작성한다. 사용자가 만족하여 최종화를 요청하면 최종 `SKILL.md`를 영어로 재작성한다.

기본 리뷰 응답은 다음 구조를 사용한다.

```md
**총평**
[현재 상태와 포트폴리오 전달력에 대한 1-3문장 요약]

**Phase 경로 해석**
- Phase 문서: [...]
- Roadmap: [...]
- Evidence: [...]
- 판단: [명확/불명확/사용자 확인 필요]

**구현 완료 여부**
- [완료/부분 완료/누락] [Phase 목표]: [구현 파일, 테스트, evidence, 판단 이유]

**Evidence 검토**
- [강함/약함/누락] [주장 또는 지표]: [근거 파일, 측정 조건, 부족한 점]

**Claim -> Evidence 매트릭스**
- [주장]: [근거 파일/지표/조건] -> [충분/부분/부족] [판단]

**report.md 완결성**
- [충분/부분/부족] [문제, 가설, 실험 조건, 결과, 해석, 한계, 다음 단계 관점 평가]

**지표 및 이상 징후**
- [정상/의심/불충분] [k6, SQL EXPLAIN ANALYZE, 로그, DB 지표 등]: [evidence 기반 판단]

**다음 Phase 준비도**
- 상태: [ready/ready after fixes/not ready]
- 이유: [...]
- 넘어가기 전 필요한 조치: [...]

**백엔드 포트폴리오 관점**
- 장점: [드러나는 백엔드 역량]
- 개선: [의도, 사고 과정, 기술 깊이를 더 명확히 하기 위한 개선점]

**면접관 예상 질문**
- [질문]: [현재 문서만으로 답변 가능 여부와 보강 포인트]

**Docusaurus portfolio.mdx**
- 상태: [바로 작성 가능/작성 전 보강 필요/작성 완료]
- 위치: [작성했거나 제안하는 경로]
- 구조: [portfolio.mdx에 들어갈 섹션]
- 시각화: [표/그래프/이미지/React chart component로 표현할 지표]

**우선순위 개선사항**
- P0: [공유 전 반드시 수정할 항목]
- P1: [중요한 개선 항목]
- P2: [완성도를 높이는 항목]
```

## 규칙

- 직접적이고 evidence 기반으로 판단한다.
- "없음"과 "있지만 불명확함"을 구분한다.
- 가능하면 파일 경로와 line number를 인용한다.
- 파일명만 보고 검토하지 않는다.
- 최종 숫자가 그럴듯해도 측정 방법론이 빠져 있으면 실제 gap으로 본다.
- 추상적 조언보다 구체적인 rewrite, checklist, artifact 제안을 우선한다.
- evidence가 부족하면 어떤 자료가 있어야 해당 주장이 방어 가능한지 말한다.
- 사용자가 명시적으로 요청하지 않는 한 프로젝트 파일을 수정하지 않는다.
- 사용자가 `portfolio.mdx` 생성을 명시적으로 요청했고 준비도가 충분하면 파일을 작성한다.
