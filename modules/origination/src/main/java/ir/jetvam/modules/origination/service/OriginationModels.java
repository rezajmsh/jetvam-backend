package ir.jetvam.modules.origination.service;

import ir.jetvam.modules.origination.model.ApplicationControlStatus;
import ir.jetvam.modules.origination.model.ApplicationStatus;
import ir.jetvam.modules.payment.service.PaymentModels;
import ir.jetvam.modules.product.model.PlanControlType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Groups application commands, customer views and background-processing results.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
public final class OriginationModels {

    private OriginationModels() {
    }

    public record CreateApplication(UUID planId, BigDecimal requestedAmount, int termMonths) {
    }

    public record GuaranteeInformation(
            String chequeSayadId,
            String bankCode,
            String chequeNumber,
            String chequeSerial,
            LocalDate chequeDate,
            BigDecimal chequeAmount,
            String guarantorNationalCode,
            String guarantorMobile
    ) {
    }

    public record ControlView(
            UUID id,
            String code,
            String title,
            int priority,
            PlanControlType type,
            String sourceInquiryCode,
            ApplicationControlStatus status,
            UUID inquiryRequestId,
            String observedValue,
            String errorMessage
    ) {
    }

    public record ApplicationView(
            UUID id,
            UUID planId,
            String planCode,
            String planName,
            BigDecimal requestedAmount,
            int termMonths,
            BigDecimal annualInterestRate,
            ApplicationStatus status,
            String rejectionReason,
            Long personalProfileRevision,
            Long employmentProfileRevision,
            List<ControlView> controls,
            List<PaymentModels.FeeView> fees,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

}
