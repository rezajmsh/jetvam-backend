package ir.jetvam.modules.inquiry.service;

import ir.jetvam.common.exception.IntegrationException;
import ir.jetvam.common.validation.Preconditions;
import ir.jetvam.modules.inquiry.model.InquiryResponseMode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Registers every inquiry first, then either returns immediately or coordinates inline execution.
 * Inline execution claims the same durable row and invokes the same executor used by the scheduled job.
 *
 * @author reza jamshidi
 * @since 9/30/2026
 */
@Service
@RequiredArgsConstructor
public class DefaultInquiryRequestService implements InquiryRequestService {

    private static final long OBSERVATION_INTERVAL_MILLIS = 100;

    private final InquiryRequestRegistrationService registrations;
    private final InquiryWorkTransactionService transactions;
    private final InquiryRequestExecutor requestExecutor;

    @Value("${jetvam.inquiry.synchronous-timeout:30s}")
    private Duration defaultTimeout;

    @Override
    public InquirySubmissionModels.Result submit(InquirySubmissionModels.Command command) {
        InquirySubmissionModels.Result submitted = registrations.register(command);
        if (command.responseMode() == InquiryResponseMode.ASYNC_CALLBACK || submitted.terminal()) {
            return submitted;
        }
        Duration timeout = Optional.ofNullable(command.timeout()).orElse(defaultTimeout);
        Preconditions.require(!timeout.isNegative() && !timeout.isZero(), "timeout must be positive");
        return executeSynchronously(command, submitted, timeout);
    }

    private InquirySubmissionModels.Result executeSynchronously(
            InquirySubmissionModels.Command command,
            InquirySubmissionModels.Result submitted,
            Duration timeout
    ) {
        long deadline = System.nanoTime() + timeout.toNanos();
        InquirySubmissionModels.Result current = submitted;
        while (System.nanoTime() < deadline) {
            if (current.terminal()) {
                return current;
            }
            Optional<AsyncInquiryModels.WorkItem> claimed = transactions.prepare(current.requestId());
            if (claimed.isPresent()) {
                AsyncInquiryModels.WorkResult execution = requestExecutor.execute(claimed.get());
                if (!execution.succeeded()) {
                    throw new IntegrationException(
                            execution.providerCode() == null ? "inquiry" : execution.providerCode(),
                            command.inquiryType().code(),
                            new IllegalStateException(execution.message())
                    );
                }
            } else {
                pause(deadline);
            }
            current = registrations.find(current.requestId());
        }
        throw new IntegrationException(
                "inquiry", command.inquiryType().code(),
                new TimeoutException("Inquiry response timed out after " + timeout)
        );
    }

    private static void pause(long deadline) {
        long remainingNanos = deadline - System.nanoTime();
        if (remainingNanos <= 0) {
            return;
        }
        long millis = Math.min(
                OBSERVATION_INTERVAL_MILLIS,
                Math.max(1, TimeUnit.NANOSECONDS.toMillis(remainingNanos))
        );
        try {
            Thread.sleep(millis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IntegrationException("inquiry", "synchronous-wait", exception);
        }
    }
}
