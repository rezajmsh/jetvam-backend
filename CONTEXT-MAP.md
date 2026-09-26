# Context Map

## Contexts

- [Identity](./modules/identity/CONTEXT.md): owns parties, accounts and authentication identity.
- [Product](./modules/product/CONTEXT.md): defines product families, selectable plans and declarative commercial requirements.
- [Inquiry](./modules/inquiry/CONTEXT.md): obtains normalized applicant facts from external inquiry providers.
- [Assessment](./modules/assessment/CONTEXT.md): evaluates authoritative applicant facts against Plan controls.
- [Origination](./modules/origination/CONTEXT.md): owns application snapshots, prioritized Control outcomes and journey stages.
- [Payment](./modules/payment/CONTEXT.md): owns fee obligations, checkout attempts and trusted confirmations.
- Integration: supplies generic runtime connectivity, routing, security and resilience for external calls; it has no business vocabulary.

## Relationships

- **Inquiry → Integration**: Inquiry owns Shahkar, bad-cheque and credit-rating capabilities and adapters; Integration executes them using provider routing and runtime TLS/authentication configuration.
- **Identity → Inquiry**: Identity consumes mobile ownership verification during customer registration.
- **Product → Assessment**: Product declares uniquely prioritized Controls and optional Inquiry fact sources; Assessment owns evaluation.
- **Assessment → Inquiry**: Assessment requests only the external facts needed by enabled Plan controls.
- **Product → Guarantee**: Product declares guarantor and collateral requirements; Guarantee owns collection and fulfillment.
- **Origination → Product**: Origination selects a Plan and must snapshot the applicable configuration for an application.
- **Origination → Identity**: Origination reads verified identity facts and reusable typed customer profile sections, retaining only the consumed profile revisions.
- **Origination → Inquiry**: Origination submits a consumer-neutral request only for the current Inquiry-backed Control; it does not own provider execution state.
- **Inquiry → Origination callback**: Inquiry delivers a terminal fact outcome through its transport abstraction. Origination's idempotent handler evaluates the correlated Control and releases only the next priority after success.
- **Origination → Assessment**: Origination supplies snapshotted controls and collected facts to the pure policy evaluator.
- **Origination → Payment**: Origination creates fee obligations and advances only after the required fee category is confirmed paid.
- **Jobs → Inquiry**: Jobs contains a thin generic handler and execution history; Inquiry owns request claims, provider tracking, retries and callback delivery.
