package ir.jetvam.apps.jobs.api;

/**
 * Requests enabling or disabling a managed job schedule.
 *
 * @author reza jamshidi
 * @since 9/23/2026
 */
public record SetJobEnabledRequest(boolean enabled) {
}
