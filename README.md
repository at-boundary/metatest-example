# antigen-example

A working example of [Antigen](https://github.com/integralquality/antigen) — a test-generation
harness reinforced by property-based fault simulation — running against a real stock-trading API.

The example exercises both parts of the system. Fault simulation evaluates a suite by mutating HTTP
responses to violate declared invariants (`price > 0`, `status == FILLED ⇒ filled_at != null`) and
recording which tests detect the violation (**caught**) and which pass regardless (**escaped**, i.e.
an assertion gap). The generation loop produces a suite from the OpenAPI specification and iterates
against that same evaluation until it detects a target fraction of the injected faults. See the
[Antigen README](https://github.com/integralquality/antigen) for the method and its validity
condition.

The hand-written suite included here is deliberately uneven in assertion depth, so the report shows a
representative spread of caught and escaped faults rather than a uniform result.

---

## Prerequisites

- Docker and Docker Compose
- Java 17+, Gradle 7.3+
- `claude` CLI on PATH — only for the generation step

## 1 — Start the demo API

Tests run against [oms-demo-api](https://github.com/integralquality/oms-demo-api), a Python/FastAPI
trading simulator. It serves `http://localhost:8000` (tests use base URI
`http://localhost:8000/api/v1`).

```bash
cd oms-demo-api
docker-compose up --build      # wait for "Application startup complete"
```

Migrations run on startup, seeding stock prices and an admin user.

## 2 — Register the test user

The tests authenticate as `test` / `test123`. Register once (persists unless you `docker-compose down -v`):

```bash
curl -X POST http://localhost:8000/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","username":"test","password":"test123","full_name":"Test User"}'
```

## 3 — Fault-simulate the suite

```bash
cd antigen-example
./gradlew test                          # normal run, no simulation
./gradlew test -DrunWithAntigen=true    # run with fault simulation
```

`-DrunWithAntigen=true` attaches the AspectJ weaver (wired by the `io.antigen` plugin) and evaluates
the suite against the invariants. Reports land in the project root:

- `antigen_report.html` — open it and start on the **Test Matrix** tab
- `fault_simulation_report.json` — machine-readable, for CI
- `schema_coverage.json` — per-endpoint response-field coverage

## 4 — Generate a suite (optional)

```bash
./gradlew generateTests
```

This reads `src/test/resources/antigen/generation/config.yml` (spec path, model, detection threshold)
and drives Claude to write tests into `src/test/java/generated/`, iterating until they pass and catch
enough injected faults. The generator sees only the OpenAPI spec — never the invariants or the
injected faults — so it can't overfit to the answer key.

---

## What's in this project

**Tests** (`src/test/java/demoapi/`) — `AuthApiTest`, `AccountsApiTest`, `OrdersApiTest`,
`PositionsApiTest`, `StocksApiTest`, `TradesApiTest`, over a shared `ApiTestBase`. They vary on
purpose: some assert on every response field, others only check the status code and array size, and a
few methods are `@Disabled` — so some faults are caught and others escape.

**Invariants** (`src/test/resources/antigen/simulation/invariants/`) — grouped by domain:
`trading-auth.yml`, `trading-accounts.yml`, `trading-orders.yml`, `trading-market.yml`. Rules marked
`# DEMO: will not be caught` are valid business rules the tests don't assert on; they surface as
escaped faults — the point being that a test exists but isn't enforcing the rule.

**Config** (`src/test/resources/antigen/`):

```
antigen/
├── antigen.properties              # io.antigen.core.config.source=local
├── simulation/
│   ├── config.yml                  # scoping & gating (excludes /accounts* from mutation)
│   ├── coverage_config.yml         # coverage + spec gap analysis
│   └── invariants/                 # the invariants above
└── generation/
    ├── config.yml                  # spec, model, fault_detection_threshold
    └── api-specs.yaml              # the OpenAPI spec
```

---

## What to expect

```
============================================================
  Antigen — Fault Simulation Summary
============================================================
  Test                                  Caught  Total  Escaped
--------------------------------------------------------------
  AccountsApiTest.testCreateAccount       11      12      1
  OrdersApiTest.testCreateBuyOrder         8      11      3
  OrdersApiTest.testListMyOrders           0       4      4
--------------------------------------------------------------
```

Tests that assert on every field catch most faults; a test that only checks `size() >= 0` catches
none, so all of its invariants escape. That contrast is what the example illustrates. Open
`antigen_report.html` → **Test Matrix** for the full breakdown.

## Resetting

```bash
cd oms-demo-api && docker-compose down -v && docker-compose up --build
# then re-register the test user (step 2)
```

## Further reading

- [Antigen README](https://github.com/integralquality/antigen) — invariants, configuration, the loop,
  and the independence principle behind the metric.
- [oms-demo-api](https://github.com/integralquality/oms-demo-api) — API reference.
