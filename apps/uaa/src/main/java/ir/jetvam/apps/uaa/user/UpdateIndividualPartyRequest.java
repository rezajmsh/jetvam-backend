package ir.jetvam.apps.uaa.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Validates administrative corrections to an individual's canonical party identity.
 * The request changes shared identity data rather than one authentication account.
 *
 * @author reza jamshidi
 * @since 9/28/2026
 */
public record UpdateIndividualPartyRequest(
        @NotBlank @Pattern(regexp = "\\d{10}") String nationalCode,
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotNull LocalDate birthDate,
        @NotBlank @Pattern(regexp = "09\\d{9}") String mobile
) {
}
