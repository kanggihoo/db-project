# Spring Profiles Guide

> Spring Boot 실행 preset과 HikariCP pool 설정을 정리한다.

## Server Presets

```bash
./scripts/server.sh pool5
./scripts/server.sh pool10
./scripts/server.sh pool20
```

| preset | Spring profile | Hikari maximum-pool-size | Hikari minimum-idle |
|---|---|---:|---:|
| `pool5` | `pool5` | 5 | 2 |
| `pool10` | `pool10` | 10 | 5 |
| `pool20` | `pool20` | 20 | 5 |

Profile files:

| File | Purpose |
|---|---|
| `ecommerce/src/main/resources/application-pool5.yaml` | 작은 pool로 대기 병목 확인 |
| `ecommerce/src/main/resources/application-pool10.yaml` | 기본 기준선 |
| `ecommerce/src/main/resources/application-pool20.yaml` | pool 확장 효과 확인 |

Hikari pool 값은 서버 재시작 후 적용된다. 같은 k6 시나리오를 pool별로 비교하려면 Spring 서버를 종료하고 다른 preset으로 다시 실행한다.

## Seeder Profiles

Seeder는 `scripts/seed.sh`가 `seeder,seed-*` profile을 조합해 실행한다.

```bash
./scripts/seed.sh small
./scripts/seed.sh loadtest
```

상세 데이터 규모는 [seed-data.md](./seed-data.md)를 기준으로 한다.

