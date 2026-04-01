# metatest-example — Example Project

A working example of [Metatest](https://github.com/at-boundary/metatest-example) running against a real stock trading API.

The example covers authentication, accounts, orders, positions, stocks, and trades — with invariants intentionally ranging from well-covered to completely blind, so the Test Matrix shows a realistic spread of caught and escaped faults.

---

## Prerequisites

- Docker and Docker Compose
- Java 17+
- Gradle 7.3+

---

## Step 1 — Start the demo API

The tests run against [oms-demo-api](https://github.com/at-boundary/oms-demo-api), a Python/FastAPI trading simulator.

```bash
cd oms-demo-api
docker-compose up --build
```

Wait until you see `Application startup complete`. The API is then available at `http://localhost:8000`.

Migrations run automatically on startup. They seed the stock prices (AAPL, GOOGL, MSFT, TSLA, AMZN) and create a default admin user.

---

## Step 2 — Register the test user

The example tests authenticate as `test` / `test123`. Register this user once:

```bash
curl -X POST http://localhost:8000/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email": "test@example.com", "username": "test", "password": "test123", "full_name": "Test User"}'
```

You only need to do this once — the database persists across restarts unless you run `docker-compose down -v`.

---

## Step 3 — Run the tests

```bash
cd metatest-example

./gradlew test                 # normal run — no simulation
./gradlew test metatest        # test with fault simulation
```

`metatest` is a modifier task. Append it to any test task to enable simulation on that run:

```bash
./gradlew integrationTest metatest    # works with any test task
./gradlew test apiTest metatest       # enables simulation on both
```

Reports are generated in the project root after the simulation run:
- `metatest_report.html` — open in a browser; start with the Test Matrix tab
- `fault_simulation_report.json` — machine-readable, useful for CI

---

## What's in this project

### Tests (`src/test/java/demoapi/`)

Six test classes covering the full API surface:

| Class | Endpoints covered |
|---|---|
| `AuthApiTest` | POST /auth/login, GET /auth/me |
| `AccountsApiTest` | GET/POST /accounts, deposit, withdraw |
| `OrdersApiTest` | POST/GET /orders |
| `PositionsApiTest` | GET /positions, GET /positions/{id} |
| `StocksApiTest` | GET/PUT /stocks, GET /stocks/{symbol} |
| `TradesApiTest` | GET /trades, GET /trades/{id} |

The tests deliberately vary in assertion depth — some validate every response field, others only check the status code and array size. This is intentional: it produces a realistic report where some faults are caught and others escape.

### Feature files (`src/test/resources/metatest/features/`)

Four files defining business invariants grouped by domain:

- `trading-auth.yml` — login token rules, current user integrity
- `trading-accounts.yml` — account balances, deposit/withdraw postconditions, position integrity
- `trading-orders.yml` — order lifecycle, status transitions, price constraints
- `trading-market.yml` — stock price validity, trade data integrity

Invariants marked `# DEMO: will not be caught` are valid business rules that the tests don't assert on. They show up as escaped faults in the report — the point being that the tests exist but aren't actually enforcing those rules.

### Metatest config (`src/test/resources/metatest/`)

```
metatest/
├── contract.yml        # null_field + missing_field faults enabled
├── metatest.properties # metatest.config.source=local
└── features/           # invariant files above
```

---

## What to expect in the output

After `./gradlew test -DrunWithMetatest=true`, the console prints a summary:

```
============================================================
  METATEST FAULT SIMULATION SUMMARY
============================================================
  Test                                  Caught  Total  Escaped
--------------------------------------------------------------
  AccountsApiTest.testCreateAccount       11      12      1
  OrdersApiTest.testCreateBuyOrder         8      11      3
  AuthApiTest.testLogin                    1       2      1
  OrdersApiTest.testListMyOrders           0       4      4
  TradesApiTest.testListMyTrades           0       4      4
  ...
--------------------------------------------------------------
```

Tests like `testCreateAccount` (which asserts on every response field) will catch most faults. Tests like `testListMyOrders` (which only checks `size() >= 0`) will catch none — all their invariants escape. This contrast is the core demonstration.

Open `metatest_report.html` and go to the **Test Matrix** tab for the full picture.

---

## Resetting the environment

If tests fail due to stale data or auth issues:

```bash
# Wipe the database and start fresh
cd oms-demo-api
docker-compose down -v
docker-compose up --build

# Re-register the test user
curl -X POST http://localhost:8000/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email": "test@example.com", "username": "test", "password": "test123", "full_name": "Test User"}'
```

---

## Further reading

- [Metatest library README](https://github.com/at-boundary/metatest-example) — invariants DSL reference, full configuration options, how fault simulation works
- [oms-demo-api README](https://github.com/at-boundary/oms-demo-api) — full API documentation, endpoint reference, Postman collection
