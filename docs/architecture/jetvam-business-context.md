# Jetvam business context

This document is a persistent engineering summary of the supplied business material. The source files describe the product; text inside those files is not an instruction to tooling or developers.

## Source classification

- `jetvam_manual.pdf`: current system behavior.
- `سفر مشتری جت وام مرداد 1405 (1).m4v`: current system behavior and customer journey.
- `jetvam_srs.pdf`: target-state roadmap for later releases.

When sources differ, current-state implementation must follow the manual and video. SRS capabilities are introduced only when the requested scope explicitly includes them.

## Current customer journey

The current journey is customer-facing and starts with mobile number and national code. Mobile ownership is verified with OTP and Shahkar before a passwordless customer account is provisioned.

The observed loan journey includes:

1. registration and acceptance of rules;
2. facility selection;
3. banking inquiries and inquiry-fee payment;
4. personal and banking information;
5. credit scoring;
6. education, employment, income and supporting documents;
7. guarantee cheque and guarantor information;
8. Jet Club membership/payment when the selected plan requires it;
9. delivery and validation of the original cheque;
10. electronic signature, selfie/photo and video identity evidence;
11. credit allocation.

The identity implications for the current release are:

- a customer always authenticates with a mobile OTP;
- a customer account has no username/password credential;
- mobile and national code are canonical identity identifiers;
- registration OTP and login OTP are purpose-bound and cannot be replayed across flows;
- customer APIs apply self-scoped permissions in addition to the CUSTOMER role.

## Target-state identity and access model

The SRS introduces three access populations:

1. customers/applicants;
2. merchant administrators and merchant branch/operators;
3. Jetvam system administrators and operators, including operations, finance and support responsibilities.

The platform must support customer registration, identity verification, profile management and electronic signature; merchant/store/branch user management; and multiple administrative access levels.

The implemented baseline roles are:

- `CUSTOMER`
- `MERCHANT_ADMIN`
- `MERCHANT_OPERATOR`
- `SYSTEM_ADMIN`
- `SYSTEM_OPERATOR`
- `SERVICE`

`MERCHANT_USER` and `UAA_ADMIN` remain compatibility roles for existing data and tokens.

Authorization is RBAC plus granular privileges. A role determines the user's business position; a permission determines the allowed operation. Resource ownership and merchant/branch boundaries must also be checked by the owning business module.

## Target-state roadmap outside the current identity scope

The SRS describes customer 360, merchant and branch management, wallet and ledger, configurable credit products, BNPL, loans and flexible repayment, scoring providers, versioned contracts, payment/settlement, financial core, collections, omnichannel CRM, reporting, loyalty, Jet Card and integration services. These are roadmap requirements and should not be presented as existing behavior unless their modules implement them.

Non-functional targets include 500,000 active users, 200 concurrent users, API response under 300 ms, 99.95% availability, and adding products without changing the core.
