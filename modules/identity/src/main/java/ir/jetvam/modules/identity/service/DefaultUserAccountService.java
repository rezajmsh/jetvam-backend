package ir.jetvam.modules.identity.service;

import ir.jetvam.common.exception.ConflictException;
import ir.jetvam.common.exception.ResourceNotFoundException;
import ir.jetvam.common.security.UserCategory;
import ir.jetvam.common.text.TextUtils;
import ir.jetvam.common.validation.IranianIdentifiers;
import ir.jetvam.common.validation.Preconditions;
import ir.jetvam.modules.identity.IdentityRoles;
import ir.jetvam.modules.identity.model.UserAccountStatus;
import ir.jetvam.modules.identity.persistence.CustomerProfileEntity;
import ir.jetvam.modules.identity.persistence.IndividualPartyEntity;
import ir.jetvam.modules.identity.persistence.PartyEntity;
import ir.jetvam.modules.identity.persistence.RoleEntity;
import ir.jetvam.modules.identity.persistence.UserAccountEntity;
import ir.jetvam.modules.identity.repository.CustomerProfileRepository;
import ir.jetvam.modules.identity.repository.IndividualPartyRepository;
import ir.jetvam.modules.identity.repository.PartyRepository;
import ir.jetvam.modules.identity.repository.RoleRepository;
import ir.jetvam.modules.identity.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implements transactional user provisioning and lifecycle management.
 * Party identity and authentication accounts remain separate persistence concepts.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */
@Service
@RequiredArgsConstructor
public class DefaultUserAccountService implements UserAccountService {

    private final PartyRepository partyRepository;
    private final IndividualPartyRepository individualRepository;
    private final UserAccountRepository userRepository;
    private final RoleRepository roleRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UserView create(CreateUserCommand command) {
        validate(command);
        String username = normalizeUsername(command.username());
        String mobile = IranianIdentifiers.normalizeMobileNumber(command.mobile());
        String nationalCode = IranianIdentifiers.normalizeNationalCode(command.nationalCode());
        rejectDuplicates(username, mobile, nationalCode);

        String displayName = command.firstName().strip() + " " + command.lastName().strip();
        IndividualPartyEntity individual = individualRepository.save(new IndividualPartyEntity(
                displayName,
                nationalCode,
                command.firstName().strip(),
                command.lastName().strip(),
                command.birthDate(),
                mobile
        ));

        Set<String> roleCodes = command.roleCodes().isEmpty()
                ? defaultRoles(command.categories())
                : command.roleCodes();
        Set<RoleEntity> roles = new LinkedHashSet<>(roleRepository.findAllByCodeIn(roleCodes));
        if (roles.size() != roleCodes.size()) {
            Set<String> found = roles.stream().map(RoleEntity::getCode).collect(Collectors.toSet());
            Set<String> missing = new LinkedHashSet<>(roleCodes);
            missing.removeAll(found);
            throw new ResourceNotFoundException("roles", missing);
        }

        UserAccountEntity account = userRepository.save(new UserAccountEntity(
                individual,
                username,
                passwordEncoder.encode(command.password()),
                command.categories(),
                roles
        ));
        if (command.categories().contains(UserCategory.CUSTOMER)) {
            customerProfileRepository.save(new CustomerProfileEntity(individual));
        }
        return toView(account, individual);
    }

    @Override
    @Transactional
    public UserView createForParty(CreateAccountForPartyCommand command) {
        Preconditions.requireNonNull(command, "command");
        Preconditions.requireNonNull(command.partyId(), "partyId");
        String username = Preconditions.requireText(command.username(), "username").strip().toLowerCase(Locale.ROOT);
        String password = Preconditions.requireText(command.password(), "password");
        Preconditions.require(password.length() >= 10, "password must contain at least 10 characters");
        Preconditions.require(!command.categories().isEmpty(), "at least one user category is required");
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new ConflictException("Username already exists", Map.of("username", username));
        }
        PartyEntity party = partyRepository.findById(command.partyId())
                .orElseThrow(() -> new ResourceNotFoundException("party", command.partyId()));
        Set<String> roleCodes = command.roleCodes().isEmpty()
                ? defaultRoles(command.categories())
                : command.roleCodes();
        Set<RoleEntity> roles = resolveRoles(roleCodes);
        UserAccountEntity account = userRepository.save(new UserAccountEntity(
                party,
                username,
                passwordEncoder.encode(password),
                command.categories(),
                roles
        ));
        IndividualPartyEntity individual = individualRepository.findById(party.getId()).orElse(null);
        return toView(account, individual);
    }

    @Override
    @Transactional(readOnly = true)
    public UserView get(UUID id) {
        UserAccountEntity account = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("user", id));
        IndividualPartyEntity individual = individualRepository.findById(account.getParty().getId()).orElse(null);
        return toView(account, individual);
    }

    @Override
    @Transactional
    public UserView changeStatus(UUID id, UserAccountStatus status) {
        Preconditions.requireNonNull(status, "status");
        UserAccountEntity account = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("user", id));
        account.changeStatus(status);
        IndividualPartyEntity individual = individualRepository.findById(account.getParty().getId()).orElse(null);
        return toView(account, individual);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean usernameExists(String username) {
        String normalized = Preconditions.requireText(username, "username").strip().toLowerCase(Locale.ROOT);
        return userRepository.existsByUsernameIgnoreCase(normalized);
    }

    private void validate(CreateUserCommand command) {
        Preconditions.requireNonNull(command, "command");
        Preconditions.requireText(command.username(), "username");
        Preconditions.require(
                IranianIdentifiers.isValidNationalCode(command.nationalCode()),
                "nationalCode is invalid"
        );
        if (TextUtils.hasText(command.mobile())) {
            Preconditions.require(
                    IranianIdentifiers.isValidMobileNumber(command.mobile()),
                    "mobile is invalid"
            );
        }
        Preconditions.requireText(command.firstName(), "firstName");
        Preconditions.requireText(command.lastName(), "lastName");
        String password = Preconditions.requireText(command.password(), "password");
        Preconditions.require(password.length() >= 10, "password must contain at least 10 characters");
        Preconditions.require(!command.categories().isEmpty(), "at least one user category is required");
    }

    private void rejectDuplicates(String username, String mobile, String nationalCode) {
        if (username != null && userRepository.existsByUsernameIgnoreCase(username)) {
            throw new ConflictException("Username already exists", Map.of("username", username));
        }
        if (mobile != null && individualRepository.existsByMobile(mobile)) {
            throw new ConflictException("Mobile number already exists", Map.of("mobile", mobile));
        }
        if (individualRepository.existsByNationalCode(nationalCode)) {
            throw new ConflictException("National code already exists", Map.of("nationalCode", nationalCode));
        }
    }

    private static String normalizeUsername(String username) {
        return TextUtils.hasText(username) ? username.strip().toLowerCase(Locale.ROOT) : null;
    }

    private static Set<String> defaultRoles(Set<UserCategory> categories) {
        Set<String> roles = new LinkedHashSet<>();
        categories.forEach(category -> roles.add(switch (category) {
            case CUSTOMER -> IdentityRoles.CUSTOMER;
            case MERCHANT -> IdentityRoles.MERCHANT_USER;
            case OPERATOR -> IdentityRoles.SYSTEM_OPERATOR;
            case SERVICE -> IdentityRoles.SERVICE;
        }));
        return roles;
    }

    private static UserView toView(UserAccountEntity account, IndividualPartyEntity individual) {
        return new UserView(
                account.getId(),
                account.getParty().getId(),
                account.getParty().getDisplayName(),
                account.getUsername(),
                individual == null ? null : individual.getMobile(),
                account.getPrimaryAuthenticationMethod(),
                account.getStatus(),
                account.getCategories(),
                account.getRoles().stream().map(RoleEntity::getCode).collect(Collectors.toUnmodifiableSet())
        );
    }

    private Set<RoleEntity> resolveRoles(Set<String> roleCodes) {
        Set<RoleEntity> roles = new LinkedHashSet<>(roleRepository.findAllByCodeIn(roleCodes));
        if (roles.size() != roleCodes.size()) {
            Set<String> found = roles.stream().map(RoleEntity::getCode).collect(Collectors.toSet());
            Set<String> missing = new LinkedHashSet<>(roleCodes);
            missing.removeAll(found);
            throw new ResourceNotFoundException("roles", missing);
        }
        return roles;
    }
}
