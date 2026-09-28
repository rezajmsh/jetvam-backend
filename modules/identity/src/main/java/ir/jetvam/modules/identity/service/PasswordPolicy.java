package ir.jetvam.modules.identity.service;

/**
 * Centralizes validation of raw passwords used by every identity workflow.
 * Raw password values are validated in memory and must never be logged.
 *
 * @author reza jamshidi
 * @since 9/28/2026
 */
public interface PasswordPolicy {

    String validate(String password);
}
