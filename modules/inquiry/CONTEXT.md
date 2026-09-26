# Inquiry

Inquiry obtains normalized applicant facts from one or more external providers without deciding product eligibility.

## Language

**Inquiry**:
A request for an authoritative applicant fact from an external source.
_Avoid_: Eligibility control, assessment

**Mobile Ownership Inquiry**:
An inquiry deciding whether a mobile number belongs to the person identified by a national code; Shahkar is a provider implementation.
_Avoid_: OTP verification, identity account

**Bad-cheque Inquiry**:
An inquiry returning the unsettled-cheque count and amount for an applicant.
_Avoid_: No-bad-cheque control, rejection decision

**Credit Rating Inquiry**:
An inquiry returning a provider rating and its normalized higher-is-better rank.
_Avoid_: Minimum credit-rank control, credit decision

**Civil-registration Inquiry**:
An inquiry returning authoritative identity validity and life status.
_Avoid_: Mobile ownership inquiry, profile data

**Military-status Inquiry**:
An inquiry returning military-service status and the normalized eligibility fact needed by a workflow.
_Avoid_: Military document, eligibility decision

**Bank-account-status Inquiry**:
An inquiry returning bank-specific status and whether the required account is active.
_Avoid_: Account balance, payment account

**Banking-facilities Inquiry**:
An inquiry returning direct, indirect and overdue facility exposure for an applicant.
_Avoid_: Loan application, credit decision

**Normalized Rank**:
A provider-independent integer whose larger value represents a stronger credit rating.
_Avoid_: Provider score, rating label

**Deferred Inquiry**:
A background inquiry execution that can either complete immediately or persist a provider tracking code for later polling.
_Avoid_: HTTP request thread, job definition

**Inquiry Request**:
A durable, consumer-neutral unit of work containing provider execution state and a callback address.
_Avoid_: Origination task, application stage

**Completion Callback**:
An at-least-once terminal outcome delivered through a configured transport and destination.
_Avoid_: Direct dependency on a consumer module

## Invariants

- Inquiry owns request status, provider affinity, tracking code, provider retries, normalized terminal facts and callback delivery retries.
- Consumers submit an inquiry code, subject identity and callback address; Inquiry never imports or names a consumer module.
- Provider calls and callback delivery occur outside database transactions.
- Callback delivery is at-least-once. A consumer must use the request and correlation identifiers to handle duplicates idempotently.
- `SPRING_BEAN` resolves an in-process `InquiryCompletionHandler` by destination key. Future MQ or webhook delivery is added by implementing `InquiryCallbackTransport`, without changing the request state machine or adding a consumer adapter module.
- The generic Inquiry job invokes `InquiryWorkerService`; it contains no Origination behavior.

For multi-step providers, the submit result records both provider code and tracking code. Polling is pinned to that provider; it is not failed over to a provider that did not create the tracking code.
