package ir.jetvam.modules.origination.service;

import ir.jetvam.modules.identity.service.CustomerProfileModels;

import java.util.List;
import java.util.UUID;

/**
 * Defines customer and operational use cases for the loan-application journey.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
public interface LoanApplicationService {

    OriginationModels.ApplicationView create(UUID customerPartyId, OriginationModels.CreateApplication command);

    List<OriginationModels.ApplicationView> findMine(UUID customerPartyId);

    OriginationModels.ApplicationView getMine(UUID customerPartyId, UUID applicationId);

    OriginationModels.ApplicationView refresh(UUID customerPartyId, UUID applicationId);

    OriginationModels.ApplicationView savePersonalInformation(
            UUID customerPartyId,
            UUID applicationId,
            CustomerProfileModels.UpdatePersonalInformation command
    );

    OriginationModels.ApplicationView saveEmploymentInformation(
            UUID customerPartyId,
            UUID applicationId,
            CustomerProfileModels.UpdateEmploymentInformation command
    );

    OriginationModels.ApplicationView saveGuaranteeInformation(
            UUID customerPartyId,
            UUID applicationId,
            OriginationModels.GuaranteeInformation command
    );

    OriginationModels.ApplicationView signContract(UUID customerPartyId, UUID applicationId);

    OriginationModels.ApplicationView markOriginalChequeReceived(UUID applicationId);

    OriginationModels.ApplicationView allocateCredit(UUID applicationId);
}
