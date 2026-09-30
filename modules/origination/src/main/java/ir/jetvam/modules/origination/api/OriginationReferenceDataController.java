package ir.jetvam.modules.origination.api;

import ir.jetvam.modules.origination.model.OriginationReferenceDataEntity;
import ir.jetvam.modules.origination.repository.OriginationReferenceDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Exposes database-managed application reference data without hard-coded category DTO fields.
 *
 * @author reza jamshidi
 * @since 9/29/2026
 */
@RestController
@RequestMapping("/api/v1/origination/reference-data")
@RequiredArgsConstructor
public class OriginationReferenceDataController {
    private final OriginationReferenceDataRepository repository;

    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER') and hasAuthority('origination:self:read')")
    public List<ReferenceGroup> find(@RequestParam List<String> categories) {
        return repository.findAllByCategoryInAndActiveTrueOrderByCategoryAscDisplayOrderAsc(categories).stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        OriginationReferenceDataEntity::getCategory,
                        java.util.LinkedHashMap::new,
                        java.util.stream.Collectors.mapping(
                                item -> new ReferenceOption(item.getCode(), item.getLabel()),
                                java.util.stream.Collectors.toList()
                        )
                )).entrySet().stream()
                .map(entry -> new ReferenceGroup(entry.getKey(), entry.getValue()))
                .toList();
    }

    public record ReferenceGroup(String category, List<ReferenceOption> options) {
    }

    public record ReferenceOption(String code, String label) {
    }
}
