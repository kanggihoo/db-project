# 004 Pagination Scenario Expansion

## Goal

Phase 7 pagination 관련 k6 scenario 파일에도 shared lib 구조를 적용한다.

## Files

- Modify: `k6/points-cursor-test.js`
- Modify: `k6/points-cursor-sampling-test.js`
- Modify: `k6/points-offset-sampling-test.js`

## Steps

- [ ] **Step 1: Inspect current pagination scenario differences**

각 파일에서 다음 요소를 먼저 확인한다.

- default preset path
- default scenario label
- request URL name tag
- fixed user/page/cursor sampling 로직
- body shape check 여부
- threshold 기본값

Run:

```bash
rtk read k6/points-cursor-test.js
rtk read k6/points-cursor-sampling-test.js
rtk read k6/points-offset-sampling-test.js
```

- [ ] **Step 2: Refactor `points-cursor-test.js`**

공통 lib를 import하고 preset/options/check plumbing을 제거한다.

유지해야 할 scenario-specific 책임:

- cursor API URL
- first page request
- `nextCursor` 추출
- next page request
- cursor response check

cursor response가 단순 `status 200`보다 더 많은 구조 검증을 한다면, 해당 검증은 scenario 파일 안에 남긴다. 공통 check helper가 scenario 의미를 숨기면 안 된다.

- [ ] **Step 3: Refactor `points-cursor-sampling-test.js`**

공통 lib를 import하고 preset/options/check plumbing을 제거한다.

유지해야 할 scenario-specific 책임:

- sampling 대상 user/cursor 선택
- cursor query parameter 구성
- request URL name tag
- response structure check

- [ ] **Step 4: Refactor `points-offset-sampling-test.js`**

공통 lib를 import하고 preset/options/check plumbing을 제거한다.

유지해야 할 scenario-specific 책임:

- offset page sampling 규칙
- user/page/size query parameter 구성
- request URL name tag

- [ ] **Step 5: Keep sampling helpers local**

이번 slice에서는 `randomBetween`, weighted page sampling, cursor sample picker 같은 helper를 새 공통 모듈로 빼지 않는다.

공통화 후보는 남겨둘 수 있지만, 이 slice의 완료 기준에는 포함하지 않는다.

- [ ] **Step 6: Verify pagination scenario files**

Run:

```bash
rtk rg -n "loadConfig|buildConstantArrivalRateOptions|GET /api/points/cursor|GET /api/points|phase-07|PRESET_NAME" k6/points-cursor-test.js k6/points-cursor-sampling-test.js k6/points-offset-sampling-test.js
rtk proxy node scripts/verify-observability.mjs
```

Expected: commands exit 0.

- [ ] **Step 7: Optional runtime smoke**

Run when environment is available:

```bash
rtk npm run k6:evidence -- --phase phase-07 --scenario points-cursor --preset points-cursor --condition refactor-smoke
```

If the repository's current scenario name differs, use the existing scenario name accepted by `k6/run.sh`.

- [ ] **Step 8: Commit**

```bash
git add k6/points-cursor-test.js k6/points-cursor-sampling-test.js k6/points-offset-sampling-test.js
git commit -m "test(k6): apply shared lib to pagination scenarios"
```
