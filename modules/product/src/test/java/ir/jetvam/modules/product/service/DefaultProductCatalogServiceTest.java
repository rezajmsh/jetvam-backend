package ir.jetvam.modules.product.service;

import ir.jetvam.common.exception.ValidationException;
import ir.jetvam.modules.product.model.ControlDefinitionEntity;
import ir.jetvam.modules.product.model.ControlParameterDefinitionEntity;
import ir.jetvam.modules.product.model.ControlSubjectType;
import ir.jetvam.modules.product.model.PlanEntity;
import ir.jetvam.modules.product.model.ProductEntity;
import ir.jetvam.modules.product.repository.CollateralTypeRepository;
import ir.jetvam.modules.product.repository.ControlDefinitionRepository;
import ir.jetvam.modules.product.repository.FeeDefinitionRepository;
import ir.jetvam.modules.product.repository.PlanRepository;
import ir.jetvam.modules.product.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies validation behavior at the product application boundary.
 *
 * @author reza jamshidi
 * @since 9/29/2026
 */
@ExtendWith(MockitoExtension.class)
class DefaultProductCatalogServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private PlanRepository planRepository;
    @Mock
    private CollateralTypeRepository collateralTypeRepository;
    @Mock
    private FeeDefinitionRepository feeDefinitionRepository;
    @Mock
    private ControlDefinitionRepository controlDefinitionRepository;
    @InjectMocks
    private DefaultProductCatalogService service;

    @Test
    void reportsMissingRequiredControlParameterAsValidationFailure() {
        UUID planId = UUID.randomUUID();
        UUID definitionId = UUID.randomUUID();
        ControlDefinitionEntity definition = mock(ControlDefinitionEntity.class);
        ControlParameterDefinitionEntity parameter = mock(ControlParameterDefinitionEntity.class);
        when(definition.isActive()).thenReturn(true);
        when(definition.getParameters()).thenReturn(Set.of(parameter));
        when(parameter.getId()).thenReturn(UUID.randomUUID());
        when(parameter.getCode()).thenReturn("MINIMUM_RANK");
        when(parameter.isRequired()).thenReturn(true);
        when(planRepository.findById(planId)).thenReturn(Optional.of(plan()));
        when(controlDefinitionRepository.findById(definitionId)).thenReturn(Optional.of(definition));

        ProductCommands.ConfigurePlan command = new ProductCommands.ConfigurePlan(
                null, List.of(), List.of(), List.of(),
                List.of(new ProductCommands.ControlRule(
                        definitionId, ControlSubjectType.APPLICANT, 1, List.of(), true
                ))
        );

        assertThatThrownBy(() -> service.configurePlan(planId, command))
                .isInstanceOfSatisfying(ValidationException.class, exception -> {
                    assertThat(exception.code()).isEqualTo("COMMON.VALIDATION_FAILED");
                    assertThat(exception.violations()).singleElement().satisfies(violation -> {
                        assertThat(violation.field()).isEqualTo("configuration");
                        assertThat(violation.code()).isEqualTo("PRODUCT.INVALID_PLAN_CONFIGURATION");
                        assertThat(violation.message()).contains("MINIMUM_RANK");
                    });
                });
    }

    private static PlanEntity plan() {
        return new PlanEntity(
                new ProductEntity("LOAN", "Loan", null),
                "PLAN", "Plan", null, BigDecimal.ZERO, BigDecimal.valueOf(100_000_000),
                6, 24, BigDecimal.valueOf(23)
        );
    }
}
