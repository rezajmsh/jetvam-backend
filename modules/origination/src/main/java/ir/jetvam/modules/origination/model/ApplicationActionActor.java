package ir.jetvam.modules.origination.model;

/**
 * Identifies who is expected to perform the next action on an application.
 *
 * @author reza jamshidi
 * @since 9/29/2026
 */
public enum ApplicationActionActor {
    CUSTOMER,
    GUARANTOR,
    SYSTEM,
    OPERATIONS,
    NONE
}
