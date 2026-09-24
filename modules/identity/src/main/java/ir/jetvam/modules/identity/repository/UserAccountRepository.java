package ir.jetvam.modules.identity.repository;

import ir.jetvam.infra.persistence.repository.JetvamJpaRepository;
import ir.jetvam.modules.identity.persistence.UserAccountEntity;
import ir.jetvam.modules.identity.model.AuthenticationMethod;

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

}
