package ru.klimovich.user_service.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import ru.klimovich.user_service.dto.request.AddressRequest;
import ru.klimovich.user_service.dto.request.UserUpdateRequest;
import ru.klimovich.user_service.dto.responce.UserResponse;
import ru.klimovich.user_service.exception.ResourceNotFoundException;
import ru.klimovich.user_service.mapper.EventMapper;
import ru.klimovich.user_service.mapper.UserMapperImpl;
import ru.klimovich.user_service.model.Address;
import ru.klimovich.user_service.model.OutboxEvent;
import ru.klimovich.user_service.model.User;
import ru.klimovich.user_service.repository.OutboxRepository;
import ru.klimovich.user_service.repository.UserRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Фиксирует baseline-поведение openspec/specs/user-profile/spec.md (as-is).
 */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    private static final String KEYCLOAK_ID = "kc-user-1";
    private static final UUID USER_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");

    @Mock
    private UserRepository userRepo;
    @Mock
    private OutboxRepository outboxRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private UserServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserServiceImpl(
                userRepo,
                new UserMapperImpl(),
                outboxRepository,
                new EventMapper(objectMapper)
        );
    }

    private Jwt jwt(Map<String, Object> claims) {
        Jwt.Builder builder = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject(KEYCLOAK_ID);
        claims.forEach(builder::claim);
        return builder.build();
    }

    private Jwt defaultJwt() {
        return jwt(Map.of(
                "given_name", "John",
                "family_name", "Doe",
                "email", "john@example.com",
                "preferred_username", "jdoe",
                "realm_access", Map.of("roles", List.of("USER", "SELLER", "offline_access",
                        "uma_authorization", "default-roles-user-service-realm"))
        ));
    }

    private User existingUser() {
        User user = new User();
        user.setId(USER_ID);
        user.setKeycloakUserId(KEYCLOAK_ID);
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("john@example.com");
        user.setPhone("+79990000000");
        return user;
    }

    private void stubSaveAssignsId() {
        when(userRepo.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(USER_ID);
            return user;
        });
    }

    // --- Profile provisioning on first read / USER_CREATED outbox event ---

    @Test
    @DisplayName("Profile provisioning on first read: профиль создается из claims JWT")
    void getUser_firstRead_createsProfileFromJwtClaims() {
        when(userRepo.findByKeycloakUserId(KEYCLOAK_ID)).thenReturn(Optional.empty());
        stubSaveAssignsId();

        service.getUser(defaultJwt());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepo).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getKeycloakUserId()).isEqualTo(KEYCLOAK_ID);
        assertThat(saved.getFirstName()).isEqualTo("John");
        assertThat(saved.getLastName()).isEqualTo("Doe");
        assertThat(saved.getEmail()).isEqualTo("john@example.com");
    }

    @Test
    @DisplayName("USER_CREATED outbox event: при создании профиля пишется одна outbox-запись")
    void getUser_firstRead_writesUserCreatedOutboxEvent() throws Exception {
        when(userRepo.findByKeycloakUserId(KEYCLOAK_ID)).thenReturn(Optional.empty());
        stubSaveAssignsId();

        service.getUser(defaultJwt());

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxRepository).save(captor.capture());
        OutboxEvent event = captor.getValue();
        assertThat(event.getAggregateType()).isEqualTo("USER");
        assertThat(event.getAggregateId()).isEqualTo(USER_ID.toString());
        assertThat(event.getEventType()).isEqualTo("USER_CREATED");
        assertThat(event.isProcessed()).isFalse();
        assertThat(event.getCreatedAt()).isNotNull();

        JsonNode payload = objectMapper.readTree(event.getPayload());
        assertThat(payload.get("userId").asText()).isEqualTo(USER_ID.toString());
        assertThat(payload.get("keycloakUserId").asText()).isEqualTo(KEYCLOAK_ID);
        assertThat(payload.get("email").asText()).isEqualTo("john@example.com");
        assertThat(payload.get("username").asText()).isEqualTo("jdoe");
    }

    @Test
    @DisplayName("Profile provisioning on first read: повторное чтение не создает профиль и событие")
    void getUser_existingProfile_doesNotSaveOrWriteOutbox() {
        when(userRepo.findByKeycloakUserId(KEYCLOAK_ID)).thenReturn(Optional.of(existingUser()));

        UserResponse response = service.getUser(defaultJwt());

        assertThat(response.getId()).isEqualTo(USER_ID);
        verify(userRepo, never()).save(any());
        verifyNoInteractions(outboxRepository);
    }

    // --- Own profile retrieval ---

    @Test
    @DisplayName("Own profile retrieval: roles из JWT без системных ролей")
    void getUser_rolesFilteredFromJwt() {
        when(userRepo.findByKeycloakUserId(KEYCLOAK_ID)).thenReturn(Optional.of(existingUser()));

        UserResponse response = service.getUser(defaultJwt());

        assertThat(response.getRoles()).containsExactly("USER", "SELLER");
    }

    @Test
    @DisplayName("Own profile retrieval: без realm_access roles пустой")
    void getUser_withoutRealmAccess_rolesEmpty() {
        when(userRepo.findByKeycloakUserId(KEYCLOAK_ID)).thenReturn(Optional.of(existingUser()));

        UserResponse response = service.getUser(jwt(Map.of("email", "john@example.com")));

        assertThat(response.getRoles()).isEmpty();
    }

    // --- Own profile update / Admin user update and deletion ---

    @Test
    @DisplayName("Own profile update: меняются только non-null поля")
    void updateUserById_changesOnlyNonNullFields() {
        User user = existingUser();
        when(userRepo.findByKeycloakUserId(KEYCLOAK_ID)).thenReturn(Optional.of(user));
        UserUpdateRequest request = new UserUpdateRequest();
        request.setPhone("+79991112233");

        UserResponse response = service.updateUserById(KEYCLOAK_ID, request);

        assertThat(user.getPhone()).isEqualTo("+79991112233");
        assertThat(user.getFirstName()).isEqualTo("John");
        assertThat(user.getLastName()).isEqualTo("Doe");
        assertThat(user.getEmail()).isEqualTo("john@example.com");
        assertThat(response.getPhone()).isEqualTo("+79991112233");
    }

    @Test
    @DisplayName("Own profile update: поля адреса меняются по отдельности")
    void updateUserById_updatesAddressFieldsIndividually() {
        User user = existingUser();
        Address address = new Address();
        address.setCountry("RU");
        address.setCity("Moscow");
        user.setAddress(address);
        when(userRepo.findByKeycloakUserId(KEYCLOAK_ID)).thenReturn(Optional.of(user));
        AddressRequest addressRequest = new AddressRequest();
        addressRequest.setCity("Kazan");
        UserUpdateRequest request = new UserUpdateRequest();
        request.setAddress(addressRequest);

        service.updateUserById(KEYCLOAK_ID, request);

        assertThat(user.getAddress().getCountry()).isEqualTo("RU");
        assertThat(user.getAddress().getCity()).isEqualTo("Kazan");
    }

    @Test
    @DisplayName("Own profile update: при отсутствии адреса создается новый")
    void updateUserById_createsAddressWhenAbsent() {
        User user = existingUser();
        when(userRepo.findByKeycloakUserId(KEYCLOAK_ID)).thenReturn(Optional.of(user));
        AddressRequest addressRequest = new AddressRequest();
        addressRequest.setCountry("RU");
        UserUpdateRequest request = new UserUpdateRequest();
        request.setAddress(addressRequest);

        service.updateUserById(KEYCLOAK_ID, request);

        assertThat(user.getAddress()).isNotNull();
        assertThat(user.getAddress().getCountry()).isEqualTo("RU");
    }

    @Test
    @DisplayName("Own profile update: email меняется только локально (OBS-03 as-is)")
    void updateUserById_changesEmailLocally() {
        User user = existingUser();
        when(userRepo.findByKeycloakUserId(KEYCLOAK_ID)).thenReturn(Optional.of(user));
        UserUpdateRequest request = new UserUpdateRequest();
        request.setEmail("new@example.com");

        service.updateUserById(KEYCLOAK_ID, request);

        assertThat(user.getEmail()).isEqualTo("new@example.com");
        assertThat(user.getKeycloakUserId()).isEqualTo(KEYCLOAK_ID);
    }

    @Test
    @DisplayName("Own profile update: несуществующий профиль -> 404")
    void updateUserById_notFound() {
        when(userRepo.findByKeycloakUserId(KEYCLOAK_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateUserById(KEYCLOAK_ID, new UserUpdateRequest()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found with id: [kc-user-1].");
    }

    @Test
    @DisplayName("Own profile deletion: удаляется только локальная запись профиля")
    void deleteUserById_deletesLocalProfile() {
        User user = existingUser();
        when(userRepo.findByKeycloakUserId(KEYCLOAK_ID)).thenReturn(Optional.of(user));

        service.deleteUserById(KEYCLOAK_ID);

        verify(userRepo).delete(user);
        verifyNoInteractions(outboxRepository);
    }

    @Test
    @DisplayName("Own profile deletion: несуществующий профиль -> 404")
    void deleteUserById_notFound() {
        when(userRepo.findByKeycloakUserId(KEYCLOAK_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteUserById(KEYCLOAK_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found with id: [kc-user-1].");
        verify(userRepo, never()).delete(any());
    }

    // --- Admin user listing and retrieval ---

    @Test
    @DisplayName("Admin user listing and retrieval: список профилей с roles = null")
    void getAllUsers_returnsProfilesWithoutRoles() {
        when(userRepo.findAll()).thenReturn(List.of(existingUser()));

        List<UserResponse> users = service.getAllUsers();

        assertThat(users).hasSize(1);
        assertThat(users.get(0).getKeycloakUserId()).isEqualTo(KEYCLOAK_ID);
        assertThat(users.get(0).getRoles()).isNull();
    }

    @Test
    @DisplayName("Admin user listing and retrieval: профиль по keycloakId с roles = null")
    void getUserByKeycloakId_returnsProfileWithoutRoles() {
        when(userRepo.findByKeycloakUserId(KEYCLOAK_ID)).thenReturn(Optional.of(existingUser()));

        UserResponse response = service.getUserByKeycloakId(KEYCLOAK_ID);

        assertThat(response.getEmail()).isEqualTo("john@example.com");
        assertThat(response.getRoles()).isNull();
    }

    @Test
    @DisplayName("Admin user listing and retrieval: несуществующий keycloakId -> 404")
    void getUserByKeycloakId_notFound() {
        when(userRepo.findByKeycloakUserId(KEYCLOAK_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getUserByKeycloakId(KEYCLOAK_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found with id: [kc-user-1].");
    }
}
