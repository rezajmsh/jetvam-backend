package ir.jetvam.modules.inquiry.service;

import tools.jackson.databind.ObjectMapper;
import ir.jetvam.common.time.ClockTimeProvider;
import ir.jetvam.common.inquiry.InquiryType;
import ir.jetvam.modules.inquiry.model.InquiryRequestEntity;
import ir.jetvam.modules.inquiry.repository.InquiryRequestRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies idempotent submission and cached-result callback delivery for asynchronous inquiries.
 *
 * @author reza jamshidi
 * @since 9/26/2026
 */
class DefaultAsyncInquiryServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-26T10:00:00Z");

    @Test
    void createsCompletedCallbackWorkFromValidCachedResult() {
        InquiryRequestRepository repository = mock(InquiryRequestRepository.class);
        InquiryDefinitionService definitions = mock(InquiryDefinitionService.class);
        var timeProvider = new ClockTimeProvider(Clock.fixed(NOW, ZoneOffset.UTC));
        InquiryRequestEntity source = InquiryRequestEntity.synchronous(
                InquiryType.BAD_CHEQUE,
                "0067749828",
                "0067749828",
                "{\"nationalCode\":\"0067749828\"}",
                NOW.minusSeconds(60)
        );
        UUID sourceId = UUID.randomUUID();
        ReflectionTestUtils.setField(source, "id", sourceId);
        source.complete(
                "BUREAU",
                "tracking-1",
                "{\"unsettledCount\":\"0\",\"totalAmount\":\"0\",\"trackingId\":\"tracking-1\"}",
                NOW.minusSeconds(50),
                NOW.plus(Duration.ofDays(1))
        );
        when(definitions.requireEnabledValidity(InquiryType.BAD_CHEQUE))
                .thenReturn(Duration.ofDays(1));
        when(repository.findByCallbackTransportAndCallbackDestinationAndCallbackCorrelationId(
                "SPRING_BEAN", "origination", "control-1"
        )).thenReturn(Optional.empty());
        when(repository.findFirstByInquiryCodeAndSubjectKeyAndStatusAndValidUntilAfterOrderByCompletedAtDesc(
                any(), any(), any(), any()
        )).thenReturn(Optional.of(source));
        when(repository.save(any(InquiryRequestEntity.class))).thenAnswer(invocation -> {
            InquiryRequestEntity saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", UUID.randomUUID());
            return saved;
        });
        DefaultAsyncInquiryService service = new DefaultAsyncInquiryService(
                repository, definitions, timeProvider, new ObjectMapper()
        );

        UUID requestId = service.submit(new AsyncInquiryModels.Submit(
                InquiryType.BAD_CHEQUE,
                "0067749828",
                new AsyncInquiryModels.Callback("SPRING_BEAN", "origination", "control-1")
        ));

        ArgumentCaptor<InquiryRequestEntity> saved = ArgumentCaptor.forClass(InquiryRequestEntity.class);
        verify(repository).save(saved.capture());
        assertThat(requestId).isEqualTo(saved.getValue().getId());
        assertThat(saved.getValue().isCacheHit()).isTrue();
        assertThat(saved.getValue().getReusedFromRequestId()).isEqualTo(sourceId);
        assertThat(saved.getValue().getResultJson()).isEqualTo(source.getResultJson());
    }
}
