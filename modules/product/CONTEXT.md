# Product

Product defines the commercial offers customers can select and the declarative requirements attached to each offer.

## Language

**Product**:
A commercial family that groups related selectable Plans.
_Avoid_: Facility request, selected offer

**Plan**:
The specific commercial offer selected by a customer, including amount, term and applicable rules.
_Avoid_: Product, loan application, level

**Inquiry Definition**:
Technical fact-source configuration referenced by one or more Plan Controls.
_Avoid_: Independently executable application requirement, eligibility result

**Guarantee Requirement**:
A Plan rule describing the number and kind of personal guarantees, including guarantors.
_Avoid_: Collateral, guarantor record

**Collateral Requirement**:
A Plan rule describing an asset or instrument required as security, such as a Sayad cheque.
_Avoid_: Guarantee, uploaded cheque

**Plan Fee**:
A charge defined by a Plan, optionally attributable to one of its Inquiry Requirements.
_Avoid_: Payment, transaction

**Plan Configuration**:
The complete set of Inquiry, Control, Guarantee, Collateral and Fee requirements attached to a Plan.
_Avoid_: Global workflow, provider configuration

**Plan Control**:
An eligibility policy attached to a Plan, such as an age range, minimum credit rank or absence of unsettled cheques. Its unique priority defines fail-fast execution order; an optional Inquiry Definition supplies external facts.
_Avoid_: Inquiry result, hardcoded workflow condition

## Invariants

- Every enabled Inquiry Definition is behind at least one enabled Plan Control.
- Control priorities are positive and unique within a Plan.
- A lower priority number executes first; later Controls are not evaluated after a failure.
- Multiple Controls may reuse facts from one Inquiry Definition without repeating the provider request.
