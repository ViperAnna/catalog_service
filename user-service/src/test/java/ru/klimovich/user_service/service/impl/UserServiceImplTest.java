//package ru.klimovich.user_service.service.impl;
//
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import ru.klimovich.user_service.dto.request.UserUpdateRequest;
//import ru.klimovich.user_service.dto.responce.UserResponse;
//import ru.klimovich.user_service.exception.ResourceNotFoundException;
//import ru.klimovich.user_service.mapper.UserMapper;
//import ru.klimovich.user_service.model.User;
//import ru.klimovich.user_service.repository.UserRepository;
//
//import java.util.List;
//import java.util.Optional;
//import java.util.UUID;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class UserServiceImplTest {
//    @Mock
//    private UserRepository userRepo;
//    @InjectMocks
//    private UserServiceImpl userService;
//    @Mock
//    private UserMapper userMapper;
//
//
////    @Test
////    void shouldCreateUser() {
////        UserRequest request = new UserRequest();
////        request.setEmail("test@mail.ru");
////        User user = new User();
////        UserResponse response = new UserResponse();
////
////        when(userRepo.findByEmail(request.getEmail()))
////                .thenReturn(Optional.empty());
////        when(userMapper.toEntity(request))
////                .thenReturn(user);
////        when(userRepo.save(user))
////                .thenReturn(user);
////        when(userMapper.toDTO(user))
////                .thenReturn(response);
////
//////        UserResponse result = userService.createUser(request);
//////        assertEquals(response, result);
////        userService.createUser(request);
////
////        verify(userRepo).findByEmail("test@mail.ru");
////        verify(userMapper).toEntity(request);
////        verify(userRepo).save(user);
////        verify(userMapper).toDTO(user);
////    }
//
////    @Test
////    void shouldThrowExceptionWhenEmailAlreadyExist() {
////        UserRequest request = new UserRequest();
////        request.setEmail("test@mail.ru");
////
////        when(userRepo.findByEmail("test@mail.ru"))
////                .thenReturn(Optional.of(new User()));
////
////        assertThrows(
////                ResourceConflictException.class,
////                () -> userService.createUser(request)
////        );
////        verify(userRepo, never()).save(any());
////    }
//
//    @Test
//    void shouldGetAllUsers() {
//        User user1 = new User();
//        User user2 = new User();
//        List<User> users = List.of(user1, user2);
//
//        UserResponse response1 = new UserResponse();
//        UserResponse response2 = new UserResponse();
//
//        when(userRepo.findAll()).thenReturn(users);
//        when(userMapper.toDTO(user1)).thenReturn(response1);
//        when(userMapper.toDTO(user2)).thenReturn(response2);
//
//        List<UserResponse> result = userService.getAllUsers();
//
//        assertEquals(2, result.size());
//        assertTrue(result.contains(response1));
//        assertTrue(result.contains(response2));
//
//        verify(userRepo).findAll();
//        verify(userMapper).toDTO(user1);
//        verify(userMapper).toDTO(user2);
//    }
//
//    @Test
//    void shouldReturnUserById() {
//
//        UUID id = UUID.randomUUID();
//        User user = new User();
//        UserResponse response = new UserResponse();
//
//        when(userRepo.findById(id)).thenReturn(Optional.of(user));
//        when(userMapper.toDTO(user)).thenReturn(response);
//
//        UserResponse result = userService.getByKeycloakId(id);
//        assertNotNull(result);
//
//        verify(userRepo).findById(id);
//        verify(userMapper).toDTO(user);
//
//    }
//
//    @Test
//    void shouldExceptionWhenUserByIdNotFound() {
//        UUID id = UUID.randomUUID();
//        when(userRepo.findById(id)).thenReturn(Optional.empty());
//
//        assertThrows(ResourceNotFoundException.class, () -> userService.getByKeycloakId(id));
//
//        verify(userRepo).findById(id);
//        verify(userMapper, never()).toDTO(any());
//    }
//
//
//    @Test
//    void shouldUpdateUserById() {
//        UUID id = UUID.randomUUID();
//        UserUpdateRequest request = new UserUpdateRequest();
//        User user = new User();
//        User updateUser = new User();
//        UserResponse response = new UserResponse();
//
//        when(userRepo.findById(id)).thenReturn(Optional.of(user));
//        doNothing().when(userMapper).updateUserFromDTO(request, user);
//        when(userRepo.save(user)).thenReturn(updateUser);
//        when(userMapper.toDTO(updateUser)).thenReturn(response);
//
//        UserResponse result = userService.updateUserById(id, request);
//
//        assertNotNull(result);
//
//        verify(userRepo).findById(id);
//        verify(userMapper).updateUserFromDTO(request, user);
//        verify(userRepo).save(user);
//        verify(userMapper).toDTO(updateUser);
//
//    }
//
//    @Test
//    void shouldThrowExceptionWhenUpdatingNonExistingUser() {
//        UUID id = UUID.randomUUID();
//        UserUpdateRequest request = new UserUpdateRequest();
//
//        when(userRepo.findById(id)).thenReturn(Optional.empty());
//
//        assertThrows(ResourceNotFoundException.class, () -> userService.updateUserById(id, request));
//
//        verify(userRepo).findById(id);
//        verify(userMapper, never()).updateUserFromDTO(any(), any());
//        verify(userRepo, never()).save(any());
//
//    }
//
//    @Test
//    void shouldDeleteUserById() {
//        UUID id = UUID.randomUUID();
//        User user = new User();
//        when(userRepo.findById(id)).thenReturn(Optional.of(user));
//
//        doNothing().when(userRepo).delete(user);
//
//        userService.deleteUserById(id);
//
//        verify(userRepo).findById(id);
//        verify(userRepo).delete(user);
//
//    }
//
//    @Test
//    void shouldThrowExceptionWhenDeleteUserNonExistingId() {
//
//        UUID id = UUID.randomUUID();
//        User user = new User();
//        when(userRepo.findById(id)).thenReturn(Optional.empty());
//
//        assertThrows(ResourceNotFoundException.class, () -> userService.deleteUserById(id));
//
//        verify(userRepo).findById(id);
//        verify(userRepo, never()).delete(any());
//    }
//}