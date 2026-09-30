package ir.jetvam.modules.identity.repository;

import ir.jetvam.infra.persistence.repository.JetvamJpaRepository;
import ir.jetvam.modules.identity.persistence.UserAccountEntity;
import ir.jetvam.modules.identity.model.AuthenticationMethod;
import ir.jetvam.modules.identity.model.UserAccountStatus;
import ir.jetvam.common.security.UserCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Loads user accounts with the identity and authorities required for authentication.
 * Login identifiers remain unique across username and mobile access paths.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */
public interface UserAccountRepository extends JetvamJpaRepository<UserAccountEntity, UUID>,
        JpaSpecificationExecutor<UserAccountEntity> {

    Optional<UserAccountEntity> findByUsernameIgnoreCaseAndPrimaryAuthenticationMethod(
            String username,
            AuthenticationMethod authenticationMethod
    );

    Optional<UserAccountEntity> findByParty_IdAndPrimaryAuthenticationMethod(
            UUID partyId,
            AuthenticationMethod authenticationMethod
    );

    boolean existsByParty_IdAndPrimaryAuthenticationMethod(UUID partyId, AuthenticationMethod authenticationMethod);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByAuthenticationMobile(String authenticationMobile);

    List<UserAccountEntity> findAllByParty_Id(UUID partyId);

    @Query("""
            select account.status as status,
                   account.authenticationChangedAt as authenticationChangedAt,
                   account.authenticationVersion as authenticationVersion
            from UserAccountEntity account
            where account.id = :id
            """)
    Optional<UserAccountSecurityState> findSecurityStateById(@Param("id") UUID id);

    @Override
    @EntityGraph(attributePaths = "party")
    Page<UserAccountEntity> findAll(Specification<UserAccountEntity> specification, Pageable pageable);

    /**
     * Builds only the predicates requested by the caller. In particular, this avoids untyped
     * {@code :parameter is null} expressions which PostgreSQL cannot reliably infer for enums.
     */
    default Page<UserAccountEntity> search(
            String text,
            UserAccountStatus status,
            UserCategory category,
            Pageable pageable
    ) {
        Specification<UserAccountEntity> specification = (root, query, criteriaBuilder) -> {
            var predicates = new ArrayList<Predicate>();

            if (text != null && !text.isBlank()) {
                String pattern = "%" + text.strip().toLowerCase(Locale.ROOT) + "%";
                var party = root.join("party", JoinType.INNER);
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("username")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(party.get("displayName")), pattern),
                        criteriaBuilder.like(root.get("authenticationMobile"), pattern)
                ));
            }
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }
            if (category != null) {
                predicates.add(criteriaBuilder.equal(root.join("categories", JoinType.INNER), category));
                query.distinct(true);
            }

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
        return findAll(specification, pageable);
    }
}
