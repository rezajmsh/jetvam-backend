# Identity

Identity defines who participates in Jetvam and how an account is associated with a verified party.

## Language

**Party**:
A person or organization represented independently from its login accounts.
_Avoid_: User, account holder

**User Account**:
The authentication identity through which a Party accesses Jetvam.
_Avoid_: Party, profile

**Customer**:
An individual applicant whose primary authentication method is mobile OTP.
_Avoid_: Panel user, password user

**Panel User**:
A system or merchant administrator/operator who authenticates with a password and may require a second factor.
_Avoid_: Customer

**Mobile Ownership Verification**:
The Inquiry-owned decision that a mobile number belongs to the person identified by a national code and that Identity consumes during registration.
_Avoid_: OTP verification, phone validation

**Customer Profile Data**:
Reusable personal, banking, contact, education, employment, income and document-reference data owned by the customer. Each section has an independent revision and is stored in typed relational fields.
_Avoid_: Application form JSON, loan-request payload snapshot

## Invariants

- A customer may read and update only the profile associated with the authenticated party identifier.
- Personal and employment profile sections are independently updateable and independently revisioned.
- Business workflows consume profile views through `CustomerProfileDataService`; they do not duplicate the payload in their own tables.
