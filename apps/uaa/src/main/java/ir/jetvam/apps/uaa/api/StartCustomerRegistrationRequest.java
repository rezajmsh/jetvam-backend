package ir.jetvam.apps.uaa.api;

import jakarta.validation.constraints.NotBlank;

/**
 * Accepts the minimal identifiers required to begin customer registration.
 * Domain validation performs Iranian identifier normalization and checksum checks.
 *
 * @author reza jamshidi
 * @since 9/22/2026
 */
public record StartCustomerRegistrationRequest(@NotBlank String mobile, @NotBlank String nationalCode) {
}
