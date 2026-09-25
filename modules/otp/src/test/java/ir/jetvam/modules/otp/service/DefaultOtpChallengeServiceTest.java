package ir.jetvam.modules.otp.service;

import ir.jetvam.common.exception.ValidationException;
import ir.jetvam.common.time.ClockTimeProvider;
import ir.jetvam.modules.notification.service.NotificationCommand;
import ir.jetvam.modules.notification.service.NotificationService;
import ir.jetvam.modules.otp.OtpPurpose;
import ir.jetvam.modules.otp.OtpStatus;
import ir.jetvam.modules.otp.config.OtpProperties;
import ir.jetvam.modules.otp.persistence.OtpChallengeEntity;
import ir.jetvam.modules.otp.persistence.OtpChallengeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies OTP hashing, purpose binding, validation, consumption and failed-attempt persistence.
 * Plaintext codes must only cross the notification delivery boundary.
 *
 * @author reza jamshidi
 * @since 9/24/2026
 */
class DefaultOtpChallengeServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-22T03:30:00Z");
    private static final OtpPurpose REGISTRATION = new OtpPurpose("REGISTRATION", "test.registration");
    private static final OtpPurpose LOGIN = new OtpPurpose("LOGIN", "test.login");
    private OtpChallengeRepository repository;
    private NotificationService notificationService;
    private DefaultOtpChallengeService service;

    @BeforeEach
    void setUp() {
        repository = mock(OtpChallengeRepository.class);
        notificationService = mock(NotificationService.class);
        OtpProperties properties = new OtpProperties();
        properties.setHmacSecret("0123456789abcdef0123456789abcdef");
        when(repository.saveAndFlush(any(OtpChallengeEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        service = new DefaultOtpChallengeService(
                repository,
                notificationService,
                properties,
                new ClockTimeProvider(Clock.fixed(NOW, ZoneOffset.UTC))
        );
    }

    @Test
    void validatesWithoutConsumingThenConsumesCorrectDeliveredCode() {
        service.issue("09121234567", "1234567890", REGISTRATION);
        ArgumentCaptor<NotificationCommand> notificationCaptor = ArgumentCaptor.forClass(NotificationCommand.class);
        verify(notificationService).enqueue(notificationCaptor.capture());
        String deliveredCode = notificationCaptor.getValue().parameters().get("code");
        ArgumentCaptor<OtpChallengeEntity> challengeCaptor = ArgumentCaptor.forClass(OtpChallengeEntity.class);
        verify(repository).saveAndFlush(challengeCaptor.capture());
        OtpChallengeEntity challenge = challengeCaptor.getValue();
        assertThat(deliveredCode).matches("\\d{6}");
        assertThat(challenge.getCodeDigest()).hasSize(64).doesNotContain(deliveredCode);

        UUID challengeId = UUID.randomUUID();
        when(repository.findByIdForUpdate(challengeId)).thenReturn(Optional.of(challenge));
        OtpVerificationData verified = service.verify(
                challengeId,
                deliveredCode,
                REGISTRATION
        );
        assertThat(verified).isEqualTo(new OtpVerificationData("09121234567", "1234567890"));
        assertThat(challenge.getStatus()).isEqualTo(OtpStatus.ACTIVE);

        OtpVerificationData consumed = service.consume(
                challengeId,
                deliveredCode,
                REGISTRATION
        );
        assertThat(consumed).isEqualTo(verified);
        assertThat(challenge.getStatus()).isEqualTo(OtpStatus.CONSUMED);
        assertThat(challenge.getConsumedAt()).isEqualTo(NOW);
    }

    @Test
    void persistsFailedAttemptBeforeReturningGenericError() {
        service.issue("09121234567", null, LOGIN);
        ArgumentCaptor<OtpChallengeEntity> challengeCaptor = ArgumentCaptor.forClass(OtpChallengeEntity.class);
        verify(repository).saveAndFlush(challengeCaptor.capture());
        OtpChallengeEntity challenge = challengeCaptor.getValue();
        UUID challengeId = UUID.randomUUID();
        when(repository.findByIdForUpdate(challengeId)).thenReturn(Optional.of(challenge));

        assertThatThrownBy(() -> service.consume(challengeId, "000000", LOGIN))
                .isInstanceOf(ValidationException.class)
                .hasMessage("OTP is invalid or expired");
        assertThat(challenge.getAttemptsRemaining()).isEqualTo(4);
        verify(repository, times(2)).saveAndFlush(challenge);
    }
}
