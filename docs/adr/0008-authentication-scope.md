# ADR-0008: Authentication Scope — Stubbed

**Status:** Accepted  
**Date:** 2026-04-28  
**Deciders:** Engineering Lead

## Context

The case study brief does not mention authentication or authorisation. Building a real
IAM layer (JWT issuance, role-based access control, OAuth2 integration) would consume
significant implementation time without demonstrating lending domain expertise, which is
what the case study evaluates.

However, the system needs *some* concept of "who performed this action" for audit trail
purposes (see [ADR-0009](0009-audit-logging-strategy.md)).

## Decision

**Authentication is stubbed.** All endpoints are open (no security filter chain). The
acting user is identified via a conventional HTTP header:

```
X-User-Id: admin-001
```

### Behaviour:

- If `X-User-Id` is present, its value is used as `performedBy` in audit log entries
  and as `createdBy` on entities.
- If `X-User-Id` is absent, the system defaults to `"system"`.
- No password, no token, no session. The header is trusted at face value.

### What is NOT built:

- Login / registration endpoints
- JWT or session-based authentication
- Role-based access control (e.g., admin vs customer)
- Endpoint-level authorisation (e.g., customer can only see their own loans)
- CSRF protection (not needed for a stateless API)

### What IS designed for future auth:

- `performedBy` field in audit log and `createdBy` on entities are `String` — ready to
  hold a real user ID from an auth provider.
- The hexagonal architecture means a real auth adapter (Spring Security filter) can be
  added to the inbound adapter layer without touching domain logic.
- All endpoints use resource-based URLs (`/customers/{id}/loans`) that map naturally
  to ownership-based authorisation rules.

## Consequences

### Positive

- Zero time spent on auth infrastructure. All time goes to lending domain.
- Audit trail still captures "who" — seed data and manual testing use meaningful
  `X-User-Id` values.
- Straightforward to add Spring Security later: configure `SecurityFilterChain`,
  extract user from JWT, remove the `X-User-Id` header convention.

### Negative

- The API is wide open. Not suitable for any environment beyond local development.
- Ownership-based access control (e.g., customer A cannot see customer B's loans) is
  not enforced. Tests must be designed knowing this.
- Reviewers must understand that `X-User-Id` is a stub, not a security mechanism.

## Alternatives Considered

| Alternative | Why rejected |
|-------------|-------------|
| **Basic Auth with hardcoded users** | Minimal effort but gives a false sense of security. Still no real authorisation. Adds friction to API testing (every curl needs `-u user:pass`). |
| **Spring Security with in-memory users** | More realistic, but still no real identity provider. Adds Spring Security configuration overhead without demonstrating lending domain skill. |
| **Full OAuth2 / JWT integration** | Correct for production but requires an identity provider (Keycloak, Auth0, etc.), token management, and RBAC design. Easily a multi-day effort that does not advance the lending domain deliverable. |
| **No user tracking at all** | Loses the audit trail. Audit is a core non-functional requirement for lending systems. The `X-User-Id` stub preserves this at near-zero cost. |
