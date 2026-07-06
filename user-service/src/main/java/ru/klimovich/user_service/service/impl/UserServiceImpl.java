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
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {
    private final UserRepository userRepo;
    private final UserMapper userMapper;
    private final OutboxRepository outboxRepository;
    private final EventMapper eventMapper;


//    @Override
//    public void createUser(UserRequest userDetails, String keycloakUserId) {
//
//        if (userRepo.existsByKeycloakUserId(keycloakUserId)) {
//            throw new ResourceConflictException(MessageKeys.USER_KEYCLOAK_ACCOUNT_EXIST);
//        }
////или моб
//        if (userRepo.findByEmail(userDetails.getEmail()).isPresent()) {
//            throw new ResourceConflictException(String.format(
//                    MessageKeys.USER_EMAIL_ALREADY_EXIST, userDetails.getEmail()));
//        }
//        if (userRepo.findByPhone(userDetails.getPhone()).isPresent()) {
//            throw new ResourceConflictException(String.format(
//                    MessageKeys.USER_PHONE_ALREADY_EXIST, userDetails.getPhone()));
//        }
//
//        User user = userMapper.toEntity(userDetails);
//        user.setKeycloakUserId(keycloakUserId);
//        userRepo.save(user);
//    }


    @Override
    public List<UserResponse> getAllUsers() {
        List<User> usersList = userRepo.findAll();
        return usersList.stream()
                .map(userMapper::toDTO)
                .toList();

    }

    @Override
    public UserResponse getUserByKeycloakId(String keycloakId) {
        User user = userRepo.findByKeycloakUserId(keycloakId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(String
                                .format(MessageKeys.USER_NOT_FOUND, keycloakId)));
        return userMapper.toDTO(user);
    }

    @Override
    @Transactional
    public UserResponse getOrCreateUser(Jwt jwt) {
        String id = jwt.getSubject();

        return userRepo.findByKeycloakUserId(jwt.getSubject())
                .map(userMapper::toDTO)
                .orElseGet(() -> {

                    User user = new User();

                    user.setKeycloakUserId(jwt.getSubject());
                    user.setFirstName(jwt.getClaimAsString("given_name"));
                    user.setLastName(jwt.getClaimAsString("family_name"));
                    user.setEmail(jwt.getClaimAsString("email"));

                    user = userRepo.save(user);

                    saveUserCreatedEvent(user, jwt);

                    return userMapper.toDTO(user);
                });
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
    public UserResponse updateUserById(String keycloakId, UserUpdateRequest userDetails) {
        User user = userRepo.findByKeycloakUserId(keycloakId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(String
                                .format(MessageKeys.USER_NOT_FOUND, keycloakId)));
        userMapper.updateUserFromDTO(userDetails, user);
        return userMapper.toDTO(userRepo.save(user));
    }

    @Override
    public void deleteUserById(String keycloakId) {
        User user = userRepo.findByKeycloakUserId(keycloakId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(String
                                .format(MessageKeys.USER_NOT_FOUND, keycloakId)));
        userRepo.delete(user);
    }
}
