package ir.jetvam.modules.inquiry.service;

import ir.jetvam.common.inquiry.InquiryType;
import ir.jetvam.common.validation.Preconditions;
import ir.jetvam.modules.inquiry.execution.InquiryExecutionResult;
import ir.jetvam.modules.inquiry.execution.TypedInquiryProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Shared provider execution use case for inline and scheduled inquiry processing.
 *
 * @author reza jamshidi
 * @since 9/30/2026
 */
@Service
public class DefaultInquiryRequestExecutor implements InquiryRequestExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger("jetvam.inquiry.executor");

    private final InquiryWorkTransactionService transactions;
    private final ObjectMapper objectMapper;
    private final Map<InquiryType, TypedInquiryProvider<?>> providers;

    public DefaultInquiryRequestExecutor(
            InquiryWorkTransactionService transactions,
            ObjectMapper objectMapper,
            List<TypedInquiryProvider<?>> providers
    ) {
        this.transactions = transactions;
        this.objectMapper = objectMapper;
        this.providers = registry(providers);
    }

    @Override
    public AsyncInquiryModels.WorkResult execute(AsyncInquiryModels.WorkItem work) {
        LOGGER.info(
                "inquiry.execution.started requestId={} inquiryCode={} responseMode={} providerCode={}",
                work.requestId(), work.inquiryCode(), work.responseMode(), work.context().providerCode()
        );
        try {
            InquiryExecutionResult result = executeProvider(work);
            transactions.complete(work.requestId(), result);
            LOGGER.info(
                    "inquiry.execution.completed requestId={} inquiryCode={} providerCode={} status={} externalReference={}",
                    work.requestId(), work.inquiryCode(), result.context().providerCode(), result.status(),
                    result.externalReference()
            );
            return new AsyncInquiryModels.WorkResult(
                    work.requestId(), AsyncInquiryModels.WorkKind.INQUIRY, work.inquiryCode(),
                    work.nationalCode(), work.subjectKey(), true, result.status().name(),
                    result.context().providerCode(), result.externalReference(), result.rejectionMessage()
            );
        } catch (RuntimeException exception) {
            transactions.fail(work.requestId(), exception);
            LOGGER.warn(
                    "inquiry.execution.failed requestId={} inquiryCode={} providerCode={} errorType={} errorMessage={}",
                    work.requestId(), work.inquiryCode(), work.context().providerCode(),
                    exception.getClass().getSimpleName(), exception.getMessage()
            );
            return new AsyncInquiryModels.WorkResult(
                    work.requestId(), AsyncInquiryModels.WorkKind.INQUIRY, work.inquiryCode(),
                    work.nationalCode(), work.subjectKey(), false, "EXECUTION_FAILED",
                    work.context().providerCode(), null, message(exception)
            );
        }
    }

    private InquiryExecutionResult executeProvider(AsyncInquiryModels.WorkItem work) {
        TypedInquiryProvider<?> provider = providers.get(work.inquiryCode());
        if (provider == null) {
            throw new IllegalArgumentException("No typed provider for inquiry: " + work.inquiryCode());
        }
        return execute(provider, work);
    }

    private <C> InquiryExecutionResult execute(
            TypedInquiryProvider<C> provider,
            AsyncInquiryModels.WorkItem work
    ) {
        try {
            C request = objectMapper.readValue(work.requestJson(), provider.requestType());
            return provider.execute(work.nationalCode(), request, work.context());
        } catch (JacksonException exception) {
            throw new IllegalArgumentException(
                    "Unable to read request for inquiry " + work.inquiryCode(), exception
            );
        }
    }

    private static Map<InquiryType, TypedInquiryProvider<?>> registry(
            List<TypedInquiryProvider<?>> candidates
    ) {
        Preconditions.requireNonNull(candidates, "providers");
        Map<InquiryType, TypedInquiryProvider<?>> result = new EnumMap<>(InquiryType.class);
        for (TypedInquiryProvider<?> provider : candidates) {
            Preconditions.requireNonNull(provider, "provider");
            TypedInquiryProvider<?> duplicate = result.put(provider.inquiryType(), provider);
            if (duplicate != null) {
                throw new IllegalStateException("Duplicate typed provider for inquiry: " + provider.inquiryType());
            }
        }
        return Map.copyOf(result);
    }

    private static String message(RuntimeException exception) {
        return exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
    }
}
