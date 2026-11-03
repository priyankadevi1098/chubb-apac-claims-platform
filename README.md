# Chubb APAC Engineering — Motor & Property Claims Platform

## Quick Start

### 1. Start Apache Kafka (Optional — Graceful Fallback Included)
```bash
docker-compose up -d
```
*(Note: If Docker is not running, the Spring Boot backend automatically falls back to local event persistence so the application remains 100% functional).*

### 2. Run the Spring Boot Backend (Port 8080)
```bash
cd backend
mvn clean spring-boot:run
```
* Run backend unit/integration tests: `mvn test`
* H2 Console: `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:chubb_claims_db`)

### 3. Run the Angular Frontend (Port 4200)
```bash
cd frontend
npm install
npm start
```
Open `http://localhost:4200` in your browser.

---

## Architecture & Design Decisions (Walkthrough Guide)

### 1. Core Entities & Domain Model
* **`Claim` (Aggregate Root):** Tracks the lifecycle across 6 APAC markets (`SG`, `HK`, `AU`, `MY`, `TH`, `JP`) and 2 lines of business (`MOTOR`, `PROPERTY`). Separates financial figures into `claimedAmountUsd`, `reserveEstimateUsd` (active liability exposure), and `approvedSettlementUsd`.
* **`ClaimNote` (Value Object Collection):** Chronological communication log capturing both claimant submissions and officer review notes.
* **`ClaimAuditEvent` (Immutable Event Log):** Populated asynchronously via the Kafka consumer (`chubb.apac.claims.lifecycle.events`).

### 2. Service Boundaries: Why a Modular Monolith?
* Built as a **Modular Monolith** with strict package boundaries (`domain`, `service`, `messaging`, `controller`).
* **Why not microservices right now?** In a 2–3 hour sprint, spinning up multiple JVM services adds deployment overhead without domain value. By decoupling state transitions via Kafka events (`ClaimEventPayload`), the Analytics/Liability projection or Notification engine can be extracted into independent microservices with zero changes to the core command pipeline.

### 3. Synchronous REST vs. Asynchronous Kafka
* **REST/HTTP (Synchronous):** Used for commands requiring immediate validation, state machine enforcement, and optimistic locking (`POST /claims`, `PATCH /assign`, `POST /request-info`, `POST /submit-info`, `POST /decision`).
* **Kafka (Asynchronous):** Used for broadcasting immutable domain transitions (`CLAIM_SUBMITTED`, `CLAIM_ASSIGNED`, `ADDITIONAL_INFO_REQUESTED`, `ADDITIONAL_INFO_PROVIDED`, `CLAIM_SETTLED`, `CLAIM_REJECTED`) keyed by `claimReference` to guarantee partition ordering per claim.

### 4. Outstanding Liability Exposure Calculation
* Open claims (`SUBMITTED`, `UNDER_REVIEW`, `INFO_REQUESTED`) hold an active `reserveEstimateUsd`.
* Upon initial FNOL submission, the system applies an actuarial reserve heuristic (`1.15x` for `PROPERTY` claims to account for hidden structural/water damage; `1.00x` for `MOTOR`).
* Claims officers can adjust the reserve during triage.
* When a claim transitions to `SETTLED` or `REJECTED`, `reserveEstimateUsd` is automatically released to `$0.00`, immediately updating the Manager Dashboard's liability exposure metrics by market and line of business.

### 5. What I Would Build With More Time
1. **Transactional Outbox Pattern:** Replace direct `kafkaTemplate.send()` inside `@Transactional` with a database outbox table + Debezium/poller to guarantee atomic DB-to-Kafka consistency.
2. **Role-Based Access Control (Spring Security + OIDC/JWT):** Enforce tenant isolation so claimants can only query their own policy claims and officers have market-scoped permissions.
3. **Document Object Storage:** Pre-signed AWS S3 / Azure Blob URLs for uploading crash photos and surveyor PDFs.
4. **Server-Sent Events (SSE) / WebSockets:** Push Kafka consumer updates directly to the Angular Manager Dashboard without polling.
