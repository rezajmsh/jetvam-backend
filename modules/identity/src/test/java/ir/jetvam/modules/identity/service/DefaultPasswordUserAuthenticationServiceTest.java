package ir.jetvam.modules.identity.service;

import ir.jetvam.common.security.UserCategory;
import ir.jetvam.common.time.ClockTimeProvider;
import ir.jetvam.modules.identity.IdentityOtpPurposes;
import ir.jetvam.modules.identity.persistence.IndividualPartyEntity;
import ir.jetvam.modules.identity.persistence.UserAccountEntity;
import ir.jetvam.modules.identity.repository.UserAccountRepository;
import ir.jetvam.modules.otp.service.OtpChallengeService;
import ir.jetvam.modules.otp.service.OtpChallengeView;
import ir.jetvam.modules.otp.service.OtpVerificationData;
import ir.jetvam.modules.settings.SettingKeys;
import ir.jetvam.modules.settings.service.SettingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies that settings enforce OTP in addition to valid password credentials.
 * Customer accounts remain excluded from the password grant.
 *
 * @author reza jamshidi
 * @since 9/23/2026
 */
class DefaultPasswordUserAuthenticationServiceTest {

    private static final String PASSWORD = "strong-password";
    private UserAccountRepository userRepository;
    private OtpChallengeService otpChallengeService;
    private SettingService settingService;
    private DefaultPasswordUserAuthenticationService service;
    private IndividualPartyEntity party;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserAccountRepository.class);
        otpChallengeService = mock(OtpChallengeService.class);
        settingService = mock(SettingService.class);
        PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        party = new IndividualPartyEntity(
                "System Operator", "1234567890", "System", "Operator", null, "09121234567"
        );
        UUID partyId = UUID.randomUUID();
        ReflectionTestUtils.setField(party, "id", partyId);
        UserAccountEntity account = new UserAccountEntity(
                party, "operator", encoder.encode(PASSWORD), Set.of(UserCategory.OPERATOR), Set.of()
        );
        when(userRepository.findByUsernameIgnoreCaseAndPrimaryAuthenticationMethod(
                org.mockito.ArgumentMatchers.eq("operator"), org.mockito.ArgumentMatchers.any()
        )).thenReturn(Optional.of(account));
        service = new DefaultPasswordUserAuthenticationService(
                userRepository,
                otpChallengeService,
                settingService,
                encoder,
                new ClockTimeProvider(Clock.fixed(Instant.parse("2026-09-23T10:00:00Z"), ZoneOffset.UTC))
        );
    }

    @Test
    void sendsAndRequiresOtpWhenSystemPolicyIsEnabled() {
        UUID challengeId = UUID.randomUUID();
        when(settingService.getBoolean(SettingKeys.SYSTEM_USER_TWO_FACTOR_REQUIRED)).thenReturn(true);
        when(otpChallengeService.issue(
                "09121234567",
                null,
                IdentityOtpPurposes.PASSWORD_LOGIN_SECOND_FACTOR
        ))
                .thenReturn(new OtpChallengeView(
                        challengeId,
                        Instant.parse("2026-09-23T10:02:00Z"),
                        Instant.parse("2026-09-23T10:01:00Z")
                ));
        when(otpChallengeService.consume(
                challengeId,
                "123456",
                IdentityOtpPurposes.PASSWORD_LOGIN_SECOND_FACTOR
        ))
                .thenReturn(new OtpVerificationData("09121234567", null));

        PasswordAuthenticationResult challengeResult = service.authenticate("OPERATOR", PASSWORD, null, null);
        PasswordAuthenticationResult authenticated = service.authenticate(
                "operator", PASSWORD, challengeId, "123456"
        );

        assertThat(challengeResult.requiresSecondFactor()).isTrue();
        assertThat(challengeResult.challenge().challengeId()).isEqualTo(challengeId);
        assertThat(authenticated.requiresSecondFactor()).isFalse();
        assertThat(authenticated.principal().categories()).containsExactly(UserCategory.OPERATOR);
        verify(otpChallengeService).consume(
                challengeId,
                "123456",
                IdentityOtpPurposes.PASSWORD_LOGIN_SECOND_FACTOR
        );
    }

    @Test
    void passwordIsSufficientWhenCategoryPolicyIsDisabled() {
        when(settingService.getBoolean(SettingKeys.SYSTEM_USER_TWO_FACTOR_REQUIRED)).thenReturn(false);

        PasswordAuthenticationResult authenticated = service.authenticate("operator", PASSWORD, null, null);

        assertThat(authenticated.requiresSecondFactor()).isFalse();
        assertThat(authenticated.challenge()).isNull();
        assertThat(authenticated.principal().categories()).containsExactly(UserCategory.OPERATOR);
    }
}
