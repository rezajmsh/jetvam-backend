# Assessment

Assessment decides whether authoritative applicant facts satisfy the enabled controls of a selected Plan.

## Language

**Plan Assessment**:
The evaluation of one applicant against all enabled controls of one active Plan.
_Avoid_: Inquiry, loan approval

**Control Outcome**:
The pass/fail result of one Plan Control together with its observed normalized value.
_Avoid_: Inquiry response, provider response

**Eligibility**:
The conjunction of all enabled Control Outcomes for a Plan at assessment time.
_Avoid_: Final loan approval, credit allocation

Controls are evaluated in ascending priority and stop at the first failure. Inquiry is a fact source for a Control, not a parallel decision workflow.
