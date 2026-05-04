# ADR-0007: Money Representation — BigDecimal + Currency Value Object

**Status:** Accepted  
**Date:** 2026-04-28  
**Deciders:** Engineering Lead

## Context

A lending platform handles money in every operation: disbursement, repayment, fee
calculation, interest computation, balance tracking, and ledger entries. Incorrect
money handling is the single highest-severity bug category in financial software.

Common pitfalls:

- **Floating-point drift:** `0.1 + 0.2 ≠ 0.3` in IEEE 754. Accumulated over thousands
  of installments, this causes real balance discrepancies.
- **Missing currency:** Adding USD to EUR without conversion is a logic error that
  compiles silently if money is just `BigDecimal`.
- **Inconsistent scale:** `BigDecimal("10")` has scale 0; `BigDecimal("10.00")` has
  scale 2. Comparison and equality behave differently.
- **Uncontrolled rounding:** Division without explicit rounding mode throws
  `ArithmeticException` in `BigDecimal`.

## Decision

All monetary values are represented by a **`Money` value object** throughout the
application. Raw `BigDecimal`, `double`, `float`, `long` (cents), or `int` are never
used for money across method boundaries.

### `Money` specification:

| Property | Value |
|----------|-------|
| `amount` | `BigDecimal`, scale = 4, rounding = `HALF_EVEN` |
| `currency` | `java.util.Currency`, default `USD` |
| Immutable | Yes — all operations return a new `Money` instance |
| Equality | `amount.compareTo()` + `currency.equals()` (not `BigDecimal.equals()`, which is scale-sensitive) |

### Arithmetic operations:

```java
Money add(Money other)        // asserts same currency
Money subtract(Money other)   // asserts same currency, asserts result ≥ 0 for balance contexts
Money multiply(BigDecimal factor)  // for percentage calculations
Money divide(BigDecimal divisor, RoundingMode mode)
boolean isGreaterThan(Money other)
boolean isZero()
```

### Database mapping:

- `amount` → `NUMERIC(19,4)` — supports values up to 999,999,999,999,999.9999
- `currency` → `VARCHAR(3)` — ISO 4217 currency code
- Mapped via JPA `@Embedded` or `AttributeConverter`

### JSON serialisation:

```json
{
  "amount": "1234.5600",
  "currency": "USD"
}
```

- Always serialised as string to avoid JavaScript floating-point issues.
- Custom Jackson `Serializer`/`Deserializer` enforces the format.

### Scale choice: 4 decimal places

| Scale | Rationale |
|-------|-----------|
| 2 | Insufficient for per-period interest calculations. Monthly rate = APR / 12 produces values like `0.0083`. Intermediate calculations need more precision than display values. |
| 4 | Sufficient for lending calculations. Covers per-period rates and fee percentages without over-consuming storage. |
| 8 | Overkill for consumer lending. Used in cryptocurrency or FX systems. |

We compute at scale 4 and round to scale 2 only at the display/statement layer.

## Consequences

### Positive

- Currency mismatch is a compile-time-adjacent error (runtime assertion in `add`/`subtract`).
- Scale is normalised — no surprises in equality checks.
- Rounding is explicit in every division, eliminating `ArithmeticException`.
- Database representation is precise and portable.
- The `Money` VO is the single place to change if we need to support multi-currency or
  adjust precision.

### Negative

- Every monetary field in domain entities, DTOs, and JPA entities uses `Money` instead
  of a primitive. This adds verbosity.
- Custom Jackson serialisation and JPA mapping must be written and tested.
- Developers must use `Money` arithmetic methods instead of operators. Java does not
  support operator overloading.

## Alternatives Considered

| Alternative | Why rejected |
|-------------|-------------|
| **Raw `BigDecimal`** | No currency context, no enforced scale, no enforced rounding. Every call site must remember the correct scale and rounding mode. This is the #1 source of money bugs. |
| **`long` in cents** (minor units) | Popular in payment processing. Eliminates floating-point issues but makes interest calculation awkward (must convert to decimal, compute, convert back). Also hard-codes the number of decimal places to the currency's minor unit, which breaks for currencies with 0 or 3 decimals (JPY, KWD). |
| **Joda-Money / JavaMoney (JSR 354)** | Well-designed library that solves the same problems. Rejected because: (a) JSR 354 has low adoption and a complex API surface; (b) Joda-Money is in maintenance mode; (c) a hand-rolled VO of ~80 lines is simpler to understand, extend, and debug than a third-party dependency. If the system grows to multi-currency with FX, adopting a library becomes worthwhile. |
| **`double` with rounding on display** | Fundamentally broken for financial calculations. Accumulated floating-point errors are undetectable until they cause real balance discrepancies. Non-negotiable rejection. |
