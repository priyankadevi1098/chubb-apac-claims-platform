# AI Working Journal — Chubb APAC Fullstack Assessment

## Entry 1: Problem Decomposition & Service Boundaries (00:00 - 00:20)
* **Prompt to AI:** "Propose a backend architecture for a multi-market APAC motor/property claims platform using Spring Boot and Kafka within a 3-hour sprint cap."
* **AI Suggestion:** Proposed 4 separate microservices (`claim-intake-service`, `workflow-service`, `notification-service`, `analytics-service`) with separate databases and Saga orchestration.
* **Challenge & Override:** Rejected the 4-microservice setup. Within a 2–3 hour sprint target, managing 4 separate Spring Boot processes and distributed transactions introduces massive operational friction with zero domain payoff.
* **Final Decision:** Built a **Modular Monolith** with clean package boundaries (`domain`, `service`, `messaging`, `controller`) and an event-driven Kafka backbone (`chubb.apac.claims.lifecycle.events`). This allows the Audit/Analytics consumer to be extracted into a standalone service later without changing the producer contract.

## Entry 2: REST vs. Kafka Communication Boundaries (00:20 - 00:45)
* **Prompt to AI:** "Generate Kafka producers and REST endpoints for claim creation, pickup, info requests, and settlement."
* **AI Suggestion:** Suggested making claim creation (`POST /claims`) asynchronous by putting the HTTP payload directly onto a Kafka queue and returning `202 Accepted` without a database write.
* **Challenge & Override:** Challenged this for the Claimant UX. When a distressed policyholder reports a motor collision or storm damage, they need immediate validation of their policy/input and an authoritative `claimReference` (`CHB-SG-2026-1001`) synchronously via HTTP `201 Created`.
* **Final Decision:**
  * **Synchronous REST/HTTP:** Used for state-mutating commands (FNOL submission, officer pickup, info request/response, settlement decision) where immediate domain invariant validation and optimistic lock checks (`@Version`) are required.
  * **Asynchronous Kafka (`chubb.apac.claims.lifecycle.events`):** Used for publishing immutable domain lifecycle events keyed by `claimReference` (guaranteeing partition ordering per claim) to drive the audit trail, downstream claimant notifications, and real-time liability exposure tracking.
  * **Resilience Override:** Added a 1.5-second timeout fallback in `ClaimEventPublisher` so if a reviewer runs the Spring Boot app locally without starting Docker/Kafka, the system logs a warning and falls back to persisting the audit event directly rather than crashing the HTTP request.

## Entry 3: Domain Model & Liability Exposure Tracking (00:45 - 01:25)
* **Prompt to AI:** "Create a JPA `Claim` entity with status and amount fields."
* **AI Suggestion:** Generated a single `amount` field on the `Claim` entity.
* **Challenge & Override:** Overrode the single amount field. Insurance claims require distinguishing between three financial figures:
  1. `claimedAmountUsd`: What the claimant asks for.
  2. `reserveEstimateUsd`: The outstanding liability exposure held on Chubb's books while the claim is open (`SUBMITTED`, `UNDER_REVIEW`, `INFO_REQUESTED`). Added an initial actuarial reserve multiplier (`1.15x` for `PROPERTY` due to hidden structural damage risk, `1.0x` for `MOTOR`) that officers can refine during review.
  3. `approvedSettlementUsd`: The final payout when `SETTLED`.
* **Final Decision:** Implemented explicit state transitions that automatically zero out `reserveEstimateUsd` when a claim reaches a terminal state (`SETTLED` or `REJECTED`), ensuring the Manager Dashboard's **Outstanding Liability Exposure** metric is always accurate in real time.

## Entry 4: Concurrency Control for Claims Staff (01:25 - 01:45)
* **Issue Identified:** Two claims officers viewing the shared queue could pick up or adjudicate the same claim simultaneously.
* **AI Suggestion:** None initially provided.
* **Direction to AI:** Instructed AI to add JPA `@Version` optimistic locking on `Claim` and map `OptimisticLockingFailureException` to `HTTP 409 Conflict` using RFC 7807 `ProblemDetail` in `GlobalExceptionHandler`.

## Entry 5: Single Angular Codebase for Dual Personas (01:45 - 02:35)
* **Prompt to AI:** "Build an Angular 17 frontend serving both Claimants and Claims Staff."
* **Accepted:** Accepted Angular 17 Standalone Components + Signals (`signal()`) for lightweight, reactive state management without NgRx boilerplate.
* **Refinement:** Structured into two dedicated feature components (`ClaimantPortalComponent` and `StaffWorkspaceComponent`) toggled via a persona switcher in the header so the panel can test the entire bidirectional loop (Claimant submits -> Officer requests info -> Claimant responds -> Officer settles -> Liability exposure updates) on a single screen in under 2 minutes.
