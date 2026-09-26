package ir.jetvam.modules.origination.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import ir.jetvam.common.time.TimeProvider;
import ir.jetvam.modules.assessment.service.AssessmentModels;
import ir.jetvam.modules.assessment.service.EligibilityPolicyEvaluator;
import ir.jetvam.modules.origination.model.ApplicationControlEntity;
import ir.jetvam.modules.origination.model.ApplicationControlStatus;
import ir.jetvam.modules.origination.model.ApplicationStatus;
import ir.jetvam.modules.origination.model.LoanApplicationEntity;
import ir.jetvam.modules.payment.service.PaymentModels;
import ir.jetvam.modules.payment.service.PaymentService;
import ir.jetvam.modules.product.model.PlanControlType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Executes application controls in strict priority order and stops on the first failure.
 * Local controls run immediately; an inquiry-backed control pauses until its asynchronous callback arrives.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
@Service
@RequiredArgsConstructor
public class OriginationControlOrchestrator {

    private final EligibilityPolicyEvaluator policyEvaluator;
    private final OriginationControlInquiryService inquiryService;
    private final PaymentService paymentService;
    private final TimeProvider timeProvider;
    private final ObjectMapper objectMapper;

    public void advance(LoanApplicationEntity application) {
        if (application.getStatus() != ApplicationStatus.WAITING_CONTROLS
                && application.getStatus() != ApplicationStatus.WAITING_CONTROL_FEE) {
            return;
        }
        while (true) {
            Optional<ApplicationControlEntity> next = application.getControls().stream()
                    .filter(control -> control.getStatus() == ApplicationControlStatus.WAITING_PRIORITY
                            || control.getStatus() == ApplicationControlStatus.BLOCKED_BY_PAYMENT)
                    .min(Comparator.comparingInt(ApplicationControlEntity::getPriority));
            if (next.isEmpty()) {
                boolean running = application.getControls().stream().anyMatch(control ->
                        control.getStatus() == ApplicationControlStatus.PENDING_INQUIRY
                                || control.getStatus() == ApplicationControlStatus.INQUIRY_SUBMITTED
                );
                if (!running && application.getControls().stream()
                        .allMatch(control -> control.getStatus() == ApplicationControlStatus.PASSED)) {
                    paymentService.activate(application.getId(), PaymentModels.APPLICATION_APPROVED);
                    application.controlsPassed();
                }
                return;
            }
            ApplicationControlEntity control = next.get();
            if (control.requiresInquiry()) {
                Optional<Map<String, String>> cachedFacts = application.getControls().stream()
                        .filter(previous -> previous.getStatus() == ApplicationControlStatus.PASSED)
                        .filter(previous -> control.getSourceInquiryCode().equals(previous.getSourceInquiryCode()))
                        .map(ApplicationControlEntity::getFactsJson)
                        .filter(json -> json != null && !json.isBlank())
                        .findFirst()
                        .map(this::readFacts);
                if (cachedFacts.isPresent()) {
                    Map<String, String> facts = cachedFacts.get();
                    apply(application, control, externalFacts(application, control, facts), facts);
                    if (control.getStatus() == ApplicationControlStatus.FAILED) {
                        reject(application, control.getErrorMessage());
                        return;
                    }
                    continue;
                }
                paymentService.activate(application.getId(), control.getSourceInquiryCode());
                if (!paymentService.allPaid(application.getId(), control.getSourceInquiryCode())) {
                    if (control.getStatus() == ApplicationControlStatus.WAITING_PRIORITY) {
                        control.blockByPayment();
                    }
                    application.waitForControlFee();
                    return;
                }
                control.releaseInquiry();
                inquiryService.submit(application, control);
                return;
            }
            apply(application, control, localFacts(application), Map.of());
            if (control.getStatus() == ApplicationControlStatus.FAILED) {
                reject(application, control.getErrorMessage());
                return;
            }
        }
    }

    public void complete(
            LoanApplicationEntity application,
            ApplicationControlEntity control,
            Map<String, String> inquiryFacts
    ) {
        try {
            apply(application, control, externalFacts(application, control, inquiryFacts), inquiryFacts);
            if (control.getStatus() == ApplicationControlStatus.FAILED) {
                reject(application, control.getErrorMessage());
            } else {
                advance(application);
            }
        } catch (RuntimeException exception) {
            technicalFailure(application, control, "Invalid inquiry facts: " + message(exception));
        }
    }

    public void reject(LoanApplicationEntity application, ApplicationControlEntity control, String reason) {
        control.fail(null, null, reason, timeProvider.now());
        reject(application, control.getErrorMessage());
    }

    public void technicalFailure(
            LoanApplicationEntity application,
            ApplicationControlEntity control,
            String reason
    ) {
        control.technicalFailure(reason, timeProvider.now());
        application.cancelRemainingControls();
        paymentService.cancelUnpaid(application.getId());
        application.requireManualReview(reason == null ? "Inquiry execution failed" : reason);
    }

    private void apply(
            LoanApplicationEntity application,
            ApplicationControlEntity control,
            AssessmentModels.Facts facts,
            Map<String, String> rawFacts
    ) {
        AssessmentModels.ControlResult result = policyEvaluator.evaluate(
                List.of(policy(control)), facts, timeProvider.today()
        ).controls().getFirst();
        String json = rawFacts.isEmpty() ? null : writeFacts(rawFacts);
        if (result.passed()) {
            control.pass(result.observedValue(), json, timeProvider.now());
        } else {
            control.fail(result.observedValue(), json, result.failureMessage(), timeProvider.now());
        }
    }

    private void reject(LoanApplicationEntity application, String reason) {
        application.cancelRemainingControls();
        paymentService.cancelUnpaid(application.getId());
        application.reject(reason == null ? "Application control failed" : reason);
    }

    private static AssessmentModels.PolicyControl policy(ApplicationControlEntity control) {
        return new AssessmentModels.PolicyControl(
                control.getControlCode(), control.getControlType(), control.getMinimumValue(),
                control.getMaximumValue(), control.getFailureMessage()
        );
    }

    private static AssessmentModels.Facts localFacts(LoanApplicationEntity application) {
        return new AssessmentModels.Facts(application.getBirthDate(), null, null);
    }

    private static AssessmentModels.Facts externalFacts(
            LoanApplicationEntity application,
            ApplicationControlEntity control,
            Map<String, String> facts
    ) {
        Integer creditRank = null;
        Boolean hasBadCheque = null;
        if (control.getControlType() == PlanControlType.MINIMUM_CREDIT_RANK) {
            creditRank = Integer.valueOf(requiredFact(facts, "rank"));
        } else if (control.getControlType() == PlanControlType.NO_BAD_CHEQUE) {
            hasBadCheque = Integer.parseInt(requiredFact(facts, "unsettledCount")) > 0;
        }
        return new AssessmentModels.Facts(application.getBirthDate(), creditRank, hasBadCheque);
    }

    private String writeFacts(Map<String, String> facts) {
        try {
            return objectMapper.writeValueAsString(facts);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Unable to persist application control facts", exception);
        }
    }

    private Map<String, String> readFacts(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Unable to reuse application control facts", exception);
        }
    }

    private static String requiredFact(Map<String, String> facts, String key) {
        String value = facts.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing inquiry fact: " + key);
        }
        return value;
    }

    private static String message(RuntimeException exception) {
        return exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
    }
}
