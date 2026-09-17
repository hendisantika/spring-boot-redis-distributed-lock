# spring-boot-redis-distributed-lock

Distributed locking in Spring Boot, demonstrated on a classic oversell problem: two application
instances racing to decrement the same product's stock.

A `POST /api/v1/product/make-order/v1` reads the stock, writes the decremented value, then sleeps for
a caller-supplied `delay` while still holding the lock. Without a lock, two concurrent orders both
read the same stock and both succeed — the stock goes negative. With the lock, the second caller
waits, re-reads the already-decremented stock, and is rejected as out of stock.

The project wires up **two** independent Redis lock implementations so you can compare them:

| Implementation | Bean | Used by |
| --- | --- | --- |
| [Redisson](https://redisson.org/) `RLock` | `RedissonClient` (`RedissonConfig`) | `ProductService#makeOrder` — the endpoint |
| Spring Integration `RedisLockRegistry` | `LockRegistry` (`SpringBootRedisDistributedLockApplication`) | `ProductService#order` — not exposed over HTTP |

Both acquire with `tryLock(5, TimeUnit.SECONDS)` and release in a `finally` block.

## Tech stack

- Java 25
- Spring Boot 4.1.1 — Web, Data JPA, Data Redis, Integration, Actuator
- Redisson 4.7.0
- PostgreSQL (H2 is on the runtime classpath but unused by default)
- Redis 7
- Testcontainers, REST Assured
- Maven

## Prerequisites

- JDK 25
- Docker (for the Redis + PostgreSQL containers)

## Getting started

Start Redis and PostgreSQL:

```bash
docker compose up -d
```

`compose.yaml` exposes Redis on `6379` and PostgreSQL on `5432` with database `product`, user `yu7i`,
password `53cret` — matching `src/main/resources/application.properties`.

Build and run:

```bash
./mvnw clean package
./mvnw spring-boot:run
```

On startup `data.sql` seeds two products (idempotently, so restarts are safe):

| id | name | stock |
| --- | --- | --- |
| 1 | Iphone | 3 |
| 2 | Macbook | 10 |

> **Note**
> `RedissonConfig` hardcodes `redis://127.0.0.1:6379`. The app fails fast at startup if Redis is not
> reachable, so bring the containers up first.

## API

### Make an order

```
POST /api/v1/product/make-order/v1
Content-Type: application/json
```

| Field | Type | Description |
| --- | --- | --- |
| `id` | `Long` | Product id to order |
| `stock` | `int` | Quantity to deduct |
| `updatedBy` | `String` | Who placed the order; stored on the product row |
| `delay` | `int` | Milliseconds to hold the lock after the update — used to widen the race window |

```bash
curl -X POST http://localhost:8080/api/v1/product/make-order/v1 \
  -H 'Content-Type: application/json' \
  -d '{"id": 1, "stock": 3, "updatedBy": "user 1", "delay": 10000}'
```

Success returns `200 OK`:

```
ORDER COMPLETED
Iphone stock count is : 0
```

Failures (product missing, or insufficient stock) are mapped by `ApplicationExceptionHandler` to
`404 Not Found` with the message as the body — e.g. `Iphone out of stock`.

## Seeing the lock work

Run two instances on different ports:

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8080
./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

Then fire both requests at the same product within the first request's `delay` window. Ready-made
requests live in `src/main/resources/` for any IDE with an HTTP client:

- `user1-8080.http` — orders 3 Iphones on `:8080` and holds the lock for 10s
- `user2-8081.http` — orders 3 Iphones on `:8081` and holds the lock for 5s

Send `user1-8080.http` first, then `user2-8081.http` while the first is still sleeping. User 1 gets
`ORDER COMPLETED`; user 2 blocks on the lock, then fails with `Iphone out of stock` because stock is
already 0.

## Tests

```bash
./mvnw test
```

`SpringBootRedisDistributedLockApplicationTests` sets up a PostgreSQL Testcontainer and REST Assured
against a random port. It currently holds only that scaffolding — no test methods yet — so the suite
reports zero tests and needs neither Docker nor Redis in CI.

## Build

[![Java CI with Maven](https://github.com/hendisantika/spring-boot-redis-distributed-lock/actions/workflows/maven.yml/badge.svg)](https://github.com/hendisantika/spring-boot-redis-distributed-lock/actions/workflows/maven.yml)

CI builds on Temurin JDK 25 via `.github/workflows/maven.yml`.

## Author

Hendi Santika

- Email: hendisantika@gmail.com
- Telegram: [@hendisantika34](https://t.me/hendisantika34)
