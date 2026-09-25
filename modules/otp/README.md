# OTP module

`jetvam-module-otp` owns challenge generation, HMAC storage, expiry, resend/rate limits, failed attempts,
single-use consumption and notification outbox enqueueing. It does not own identity, login or contract business rules.

Each consumer defines its own `OtpPurpose` with a stable code and notification template. This lets modules such as
identity or contract add purposes without changing the OTP table or module implementation.

For workflows that call an external service:

1. Call `verify` in its short transaction.
2. Perform external I/O after the method returns.
3. Call `consumeAtomically` from the local completion transaction.

For ordinary authentication without external I/O, call `consume`; it uses its own transaction so failed-attempt and
expiry state are preserved even when the surrounding authentication fails.
