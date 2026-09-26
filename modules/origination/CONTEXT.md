# Origination

Origination owns the lifecycle of a customer's request for one selected Plan.

## Language

**Loan Application**:
The customer's request with an immutable snapshot of the selected Plan and authoritative identity facts.
_Avoid_: Product configuration, facility account

**Application Control**:
A persisted snapshot and runtime outcome of one prioritized Plan Control. It can run locally or wait for an Inquiry callback.
_Avoid_: Independent application inquiry, provider adapter

**Application Stage**:
A state-machine gate such as inquiry fee, inquiries, personal information, employment, guarantee, application fee, original cheque, signature or credit allocation.
_Avoid_: Frontend page index

## Invariants

- Only the owning customer can read or mutate customer stages.
- Plan configuration is snapshotted at creation time.
- Personal and employment data remain owned by Identity; an application stores only the consumed section revisions.
- Completed customer profile sections are reused and skipped in later application journeys.
- External calls never occur inside database transactions.
- Every enabled Control and its initial status are persisted when the application is created.
- Controls execute one at a time in ascending unique priority. The first failed Control rejects the application and cancels every later Control before it can incur provider work or cost.
- A Control's Inquiry fees remain blocked until that Control reaches the head of the priority queue. Rejection or exhausted technical failure cancels all unpaid fees; final application fees activate only after every Control passes.
- Origination owns no provider execution, tracking or retry state; an Inquiry code is an optional fact source behind the current Control.
- The Spring Bean completion handler is idempotent, locks the owning application aggregate and updates the correlated Control.
- Facts are reused when later Controls reference the same Inquiry code.
- An exhausted technical Inquiry failure sends the application to manual review and cancels later Controls.
