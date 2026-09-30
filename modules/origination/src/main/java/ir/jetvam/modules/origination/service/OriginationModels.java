package ir.jetvam.modules.origination.service;

import ir.jetvam.modules.origination.model.ApplicationControlStatus;
import ir.jetvam.modules.origination.model.ApplicationStatus;
import ir.jetvam.modules.origination.model.ApplicationGuarantorStatus;
import ir.jetvam.modules.origination.model.ApplicationCollateralStatus;
import ir.jetvam.modules.origination.model.ApplicationJourneyStage;
import ir.jetvam.modules.origination.model.ApplicationJourneyStageStatus;
import ir.jetvam.modules.origination.model.ApplicationActionActor;
import ir.jetvam.modules.origination.model.CollateralProviderType;
import ir.jetvam.modules.payment.service.PaymentModels;
import ir.jetvam.modules.product.model.PlanControlType;
import ir.jetvam.modules.product.model.ControlSubjectType;

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

    public record EmploymentInformation(
            String educationCode,
            String employmentCode,
            BigDecimal monthlyIncome,
            List<UUID> documentIds
    ) {
    }

    public record ControlView(
            UUID id,
            String code,
            String title,
            int priority,
            PlanControlType type,
            ControlSubjectType subjectType,
            UUID subjectPartyId,
            String sourceInquiryCode,
            ApplicationControlStatus status,
            UUID inquiryRequestId,
            String observedValue,
            String errorMessage
    ) {
    }

    public record GuarantorView(
            UUID id, int sequenceNumber, boolean required, UUID partyId, String nationalCode, String mobile,
            ApplicationGuarantorStatus status
    ) {
    }

    public record CollateralView(
            UUID id, CollateralProviderType providerType, UUID providerPartyId, UUID guarantorId,
            UUID collateralTypeId, String collateralTypeCode, String collateralTypeTitle, String handlerCode,
            BigDecimal minimumCoveragePercent, boolean required, boolean requiresPhysicalDelivery,
            ApplicationCollateralStatus status, List<CollateralDocumentRequirementView> documentRequirements
    ) {
    }

    public record CollateralDocumentRequirementView(
            UUID id, UUID documentTypeId, String documentTypeCode, String documentTypeTitle,
            String allowedContentTypes, long maximumSizeBytes, boolean required,
            int minimumCount, int maximumCount, int uploadedCount, boolean satisfied,
            List<ApplicationDocumentView> documents
    ) {
    }

    public record ApplicationDocumentView(
            UUID id, String originalFilename, String contentType, long sizeBytes,
            String checksumSha256, Instant createdAt
    ) {
    }

    public record UploadDocument(String originalFilename, String contentType, byte[] content) {
    }

    public record JourneyStageView(
            ApplicationJourneyStage code, String title, String description, ApplicationJourneyStageStatus status
    ) {
    }

    public record CurrentActionView(
            String code, String title, String description, ApplicationActionActor actor,
            boolean customerActionRequired
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
            EmploymentInformation employmentInformation,
            List<ControlView> controls,
            List<GuarantorView> guarantors,
            List<CollateralView> collaterals,
            List<PaymentModels.FeeView> fees,
            List<JourneyStageView> journey,
            CurrentActionView currentAction,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

}
