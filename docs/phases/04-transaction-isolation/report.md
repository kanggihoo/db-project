# Phase 4 결과 보고서

## 寃곕줎

Phase 4??Testcontainers PostgreSQL怨?JDBC connection ??媛쒕? 吏곸젒 ?쒖뼱?섎뒗 integration test濡?寃⑸━ ?섏?蹂?媛?쒖꽦 李⑥씠瑜??ы쁽?덈떎.

PostgreSQL?먯꽌??`READ UNCOMMITTED`瑜??붿껌?대룄 Dirty Read媛 諛쒖깮?섏? ?딆븯?? `READ COMMITTED`?먯꽌??Non-Repeatable Read? Phantom Read媛 ?ы쁽?먭퀬, `REPEATABLE READ`?먯꽌???몃옖??뀡 ?쒖옉 ?쒖젏 snapshot???좎??섏뼱 ???꾩긽??諛⑹??먮떎.

Lost Update??naive read-modify-write ?⑦꽩?먯꽌 `READ COMMITTED`濡??ы쁽?먮떎. 媛숈? ?⑦꽩??`REPEATABLE READ`?먯꽌 ?ㅽ뻾?섎㈃ PostgreSQL??SQLSTATE `40001` concurrent update failure濡?stale write瑜??ㅽ뙣?쒖폒 Lost Update媛 議곗슜??諛쒖깮?섏? ?딆븯??

## 격리 수준별 비교

| Isolation Level | Dirty Read | Non-Repeatable Read | Phantom Read | Lost Update / Concurrent Update | Notes |
|---|---|---|---|---|---|
| READ COMMITTED | 諛⑹? | 諛쒖깮 | 諛쒖깮 | naive read-modify-write?먯꽌 諛쒖깮 媛??| PostgreSQL 湲곕낯 寃⑸━ ?섏? |
| REPEATABLE READ | 諛⑹? | 諛⑹? | 諛⑹? | SQLSTATE `40001` concurrent update failure濡?諛⑹? | PostgreSQL MVCC snapshot ?뺤씤 |
| SERIALIZABLE | 諛⑹? | 諛⑹? | 諛⑹? | serialization failure濡?諛⑹? | ?대쾲 援ы쁽 ?뚯뒪?몄뿉?쒕뒗 臾몄꽌??鍮꾧탳 ??곸쑝濡??좎? |

## Phase 5 Handoff

트랜잭션 격리 수준별 가시성과 동시 갱신 충돌 경계를 정리한 뒤, Phase 5에서는 조회 레이어의 DTO projection과 동적 조건 조합을 다룬다. Atomic UPDATE, 비관적 락, 낙관적 락, retry, idempotency 전략 비교는 Phase 11로 넘긴다.
