package ir.jetvam.modules.identity.service;

/**
 * Verifies ownership consistency between an Iranian mobile and national code.
 * Provider-specific transport and credentials remain outside the identity domain.
 *
 * @author reza jamshidi
 * @since 9/22/2026
 */
public interface ShahkarProvider {

    ShahkarVerification verify(String mobile, String nationalCode);
}
