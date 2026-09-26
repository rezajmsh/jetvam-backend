# Payment

Payment owns amounts payable by a customer and the attempts used to settle them.

## Language

**Fee Obligation**:
An immutable amount, currency and category linked to a business reference.
_Avoid_: Plan fee rule, application stage

**Payment Attempt**:
An idempotent checkout attempt for one fee obligation.
_Avoid_: Fee obligation, transaction ledger

**Trusted Confirmation**:
A provider-verified or operational callback that marks an attempt and its fee successful.
_Avoid_: Customer confirmation

Inquiry and application fees are separate categories so Origination can gate the corresponding stages independently.

Fees may also have an activation key. A fee for an Inquiry-backed Control stays `BLOCKED` until that Control reaches its priority; the final application fee uses `APPLICATION_APPROVED`. Failed eligibility cancels every unpaid fee so later provider work cannot create avoidable customer cost.
