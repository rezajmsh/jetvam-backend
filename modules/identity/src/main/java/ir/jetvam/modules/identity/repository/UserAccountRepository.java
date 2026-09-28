package ir.jetvam.modules.identity.repository;

import ir.jetvam.infra.persistence.repository.JetvamJpaRepository;
import ir.jetvam.modules.identity.persistence.UserAccountEntity;
import ir.jetvam.modules.identity.model.AuthenticationMethod;
import ir.jetvam.modules.identity.model.UserAccountStatus;
import ir.jetvam.common.security.UserCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/**
 * Loads user accounts with the identity and authorities required for authentication.
 * Login identifiers remain unique across username and mobile access paths.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */
public interface UserAccountRepository extends JetvamJpaRepository<UserAccountEntity, UUID> {

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

    @Query("""
            select account.status as status,
                   account.authenticationChangedAt as authenticationChangedAt,
                   account.authenticationVersion as authenticationVersion
            from UserAccountEntity account
            where account.id = :id
            """)
    Optional<UserAccountSecurityState> findSecurityStateById(@Param("id") UUID id);

    @EntityGraph(attributePaths = "party")
    @Query(
            value = """
                    select account
                    from UserAccountEntity account
                    join account.party party
                    where (:text is null
                        or lower(coalesce(account.username, '')) like lower(concat('%', :text, '%'))
                        or lower(party.displayName) like lower(concat('%', :text, '%'))
                        or account.authenticationMobile like concat('%', :text, '%'))
                      and (:status is null or account.status = :status)
                      and (:category is null or :category member of account.categories)
                    """,
            countQuery = """
                    select count(account)
                    from UserAccountEntity account
                    join account.party party
                    where (:text is null
                        or lower(coalesce(account.username, '')) like lower(concat('%', :text, '%'))
                        or lower(party.displayName) like lower(concat('%', :text, '%'))
                        or account.authenticationMobile like concat('%', :text, '%'))
                      and (:status is null or account.status = :status)
                      and (:category is null or :category member of account.categories)
                    """
    )
    Page<UserAccountEntity> search(
            @Param("text") String text,
            @Param("status") UserAccountStatus status,
            @Param("category") UserCategory category,
            Pageable pageable
    );
}
