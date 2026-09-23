package ir.jetvam.infra.i18n.persistence;

import ir.jetvam.infra.persistence.repository.JetvamJpaRepository;

import java.util.List;

/**
 * Loads active localized messages through the shared Jetvam JPA repository contract.
 * Calls are automatically covered by repository observability instrumentation.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */
public interface I18nMessageJpaRepository extends JetvamJpaRepository<I18nMessageEntity, Long> {

    List<I18nMessageEntity> findAllByActiveTrueAndLocaleIgnoreCaseOrderByMessageKey(String locale);
}
