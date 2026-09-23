package ir.jetvam.apps.uaa.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Accepts personal fields supplied after successful customer registration.
 * The authenticated account determines which profile may be modified.
 *
 * @author reza jamshidi
 * @since 9/22/2026
 */
public record CompleteCustomerProfileRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotNull LocalDate birthDate
) {
}
