# Troubleshooting

## PowerShell에서 한글이 깨질 때

원인: 기본 인코딩으로 Markdown 파일을 읽으면 UTF-8 한글이 깨질 수 있다.

확인/해결:

```powershell
Get-Content -LiteralPath .memory/README.md -Encoding UTF8
Get-Content -LiteralPath docs/PHASE1-OVERVIEW.md -Encoding UTF8
```

## Docker 재기동 후 init.sql이 다시 실행되지 않을 때

원인: PostgreSQL Docker 볼륨이 이미 존재하면 `/docker-entrypoint-initdb.d/init.sql`은 재실행되지 않는다.

해결:

```powershell
docker-compose down -v
docker-compose up -d
```

주의: `down -v`는 DB 데이터를 삭제한다. Phase 측정 데이터가 필요한 경우 먼저 보존 여부를 확인한다.

## pg_stat_statements가 보이지 않을 때

확인:

```sql
SELECT * FROM pg_extension WHERE extname = 'pg_stat_statements';
SELECT * FROM pg_stat_statements LIMIT 1;
```

원인 후보:

- PostgreSQL이 `shared_preload_libraries=pg_stat_statements` 없이 시작됨
- Docker 볼륨이 이미 있어서 수정된 init 설정이 반영되지 않음
- 테스트 컨테이너 환경에서 extension 생성이 제한됨

## Phase 1 측정값이 흔들릴 때

확인 사항:

- `show-sql=false`
- Hikari maximum pool size 10
- 부하테스트 전 `VACUUM ANALYZE`
- k6 조건 VU 50, 5분, 30초 Ramp-up, timeout 5초 유지
- 본 측정 전 API별 Warm-up 요청 2~3회 수행

## 시더가 데이터를 다시 넣지 않을 때

원인: `DataSeeder`가 users count를 확인해 이미 데이터가 있으면 삽입을 건너뛰도록 되어 있다.

해결:

- 기존 데이터를 유지하려면 그대로 둔다.
- 재시딩이 필요하면 Docker 볼륨 삭제 후 인프라를 다시 올린다.
