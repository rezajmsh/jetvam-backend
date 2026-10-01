package ir.jetvam.modules.inquiry.service;

import ir.jetvam.common.inquiry.InquiryType;
import ir.jetvam.modules.inquiry.execution.InquiryExecutionContext;
import ir.jetvam.modules.inquiry.execution.InquiryExecutionResult;
import ir.jetvam.modules.inquiry.execution.TypedInquiryProvider;
import ir.jetvam.modules.inquiry.model.InquiryResponseMode;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Verifies that the shared request executor deserializes and dispatches directly to a typed provider.
 *
 * @author reza jamshidi
 * @since 10/1/2026
 */
class DefaultInquiryRequestExecutorTest {

    @Test
    void dispatchesDirectlyToTypedProviderAndPersistsItsResult() {
        InquiryWorkTransactionService transactions = mock(InquiryWorkTransactionService.class);
        InquiryExecutionContext context = new InquiryExecutionContext("PROVIDER_A", Map.of("step", "NEXT"));
        InquiryExecutionResult expected = InquiryExecutionResult.completed(
                context, "reference-1", Map.of("unsettledCount", "0", "totalAmount", "0")
        );
        TypedInquiryProvider<InquiryRequests.BadCheque> provider = new TypedInquiryProvider<>() {
            @Override
            public InquiryType inquiryType() {
                return InquiryType.BAD_CHEQUE;
            }

            @Override
            public Class<InquiryRequests.BadCheque> requestType() {
                return InquiryRequests.BadCheque.class;
            }

            @Override
            public InquiryExecutionResult execute(
                    String nationalCode,
                    InquiryRequests.BadCheque request,
                    InquiryExecutionContext executionContext
            ) {
                assertThat(nationalCode).isEqualTo("0067749828");
                assertThat(request.nationalCode()).isEqualTo("0067749828");
                assertThat(executionContext).isEqualTo(context);
                return expected;
            }
        };
        DefaultInquiryRequestExecutor executor = new DefaultInquiryRequestExecutor(
                transactions, new ObjectMapper(), List.of(provider)
        );
        UUID requestId = UUID.randomUUID();
        AsyncInquiryModels.WorkItem work = new AsyncInquiryModels.WorkItem(
                requestId,
                InquiryType.BAD_CHEQUE,
                "0067749828",
                "0067749828",
                "{\"nationalCode\":\"0067749828\"}",
                InquiryResponseMode.SYNCHRONOUS,
                context
        );

        AsyncInquiryModels.WorkResult result = executor.execute(work);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.providerCode()).isEqualTo("PROVIDER_A");
        assertThat(result.externalReference()).isEqualTo("reference-1");
        verify(transactions).complete(requestId, expected);
    }
}
