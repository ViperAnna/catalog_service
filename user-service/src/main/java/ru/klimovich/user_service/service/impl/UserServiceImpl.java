package ru.klimovich.user_service.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.klimovich.user_service.dto.request.UserUpdateRequest;
import ru.klimovich.user_service.dto.responce.UserResponse;
import ru.klimovich.user_service.event.UserCreatedEvent;
import ru.klimovich.user_service.exception.ResourceNotFoundException;
import ru.klimovich.user_service.mapper.EventMapper;
import ru.klimovich.user_service.mapper.UserMapper;
import ru.klimovich.user_service.model.OutboxEvent;
import ru.klimovich.user_service.model.User;
import ru.klimovich.user_service.repository.OutboxRepository;
import ru.klimovich.user_service.repository.UserRepository;
import ru.klimovich.user_service.service.UserService;
import ru.klimovich.user_service.util.MessageKeys;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepo;
    private final UserMapper userMapper;
    private final OutboxRepository outboxRepository;
    private final EventMapper eventMapper;
    private static final Set<String> SYSTEM_ROLES = Set.of(
            "offline_access",
            "uma_authorization",
            "default-roles-user-service-realm"
    );

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        List<User> usersList = userRepo.findAll();
        return usersList.stream()
                .map(userMapper::toDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserByKeycloakId(String keycloakId) {
        User user = userRepo.findByKeycloakUserId(keycloakId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(String
                                .format(MessageKeys.USER_NOT_FOUND, keycloakId)));
        return userMapper.toDTO(user);
    }

    @Override
    @Transactional
    public UserResponse getUser(Jwt jwt) {

        User user = userRepo.findByKeycloakUserId(jwt.getSubject())
                .orElseGet(() -> createUser(jwt));

        UserResponse response = userMapper.toDTO(user);
        response.setRoles(getRoles(jwt));
        return response;
    }

    private User createUser(Jwt jwt) {

        User user = new User();

        user.setKeycloakUserId(jwt.getSubject());
        user.setFirstName(jwt.getClaimAsString("given_name"));
        user.setLastName(jwt.getClaimAsString("family_name"));
        user.setEmail(jwt.getClaimAsString("email"));

        user = userRepo.save(user);

        saveUserCreatedEvent(user, jwt);

        return user;

    }

    private List<String> getRoles(Jwt jwt) {

        Map<String, Object> realmAccess = jwt.getClaim("realm_access");

        if (realmAccess == null) {
            return List.of();
        }

        Object roles = realmAccess.get("roles");

        if (!(roles instanceof List<?> list)) {
            return List.of();
        }
        return list.stream()
                .map(Object::toString)
                .filter(role -> !SYSTEM_ROLES.contains(role))
                .toList();
    }

    private void saveUserCreatedEvent(User user, Jwt jwt) {
        UserCreatedEvent event = UserCreatedEvent.builder()
                .userId(user.getId().toString())
                .keycloakUserId(jwt.getSubject())
                .email(user.getEmail())
                .username(jwt.getClaimAsString("preferred_username"))
                .build();

        outboxRepository.save(
                OutboxEvent.builder()
                        .id(UUID.randomUUID())
                        .aggregateType("USER")
                        .aggregateId(user.getId().toString())
                        .eventType("USER_CREATED")
                        .payload(eventMapper.toJson(event))
                        .createdAt(LocalDateTime.now())
                        .processed(false)
                        .build()
        );
    }

    @Override
    @Transactional
    public UserResponse updateUserById(String keycloakId, UserUpdateRequest userDetails) {
        User user = userRepo.findByKeycloakUserId(keycloakId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(String
                                .format(MessageKeys.USER_NOT_FOUND, keycloakId)));
        userMapper.updateUserFromDTO(userDetails, user);
        return userMapper.toDTO(user);
    }

    @Override
    @Transactional
    public void deleteUserById(String keycloakId) {
        User user = userRepo.findByKeycloakUserId(keycloakId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(String
                                .format(MessageKeys.USER_NOT_FOUND, keycloakId)));
        userRepo.delete(user);
    }
}
