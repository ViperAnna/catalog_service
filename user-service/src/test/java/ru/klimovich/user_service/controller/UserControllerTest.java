//package ru.klimovich.user_service.controller;
//
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
//import org.springframework.boot.test.mock.mockito.MockBean;
//import org.springframework.http.MediaType;
//import org.springframework.test.web.servlet.MockMvc;
//import ru.klimovich.user_service.dto.responce.UserResponse;
//import ru.klimovich.user_service.exception.ResourceNotFoundException;
//import ru.klimovich.user_service.service.UserService;
//import ru.klimovich.user_service.util.MessageKeys;
//
//import java.util.List;
//import java.util.UUID;
//
//import static org.mockito.Mockito.*;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
//@WebMvcTest(UserController.class)
//@ExtendWith(MockitoExtension.class)
//class UserControllerTest {
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @MockBean
//    private UserService userService;
//
////    @Test
////    void shouldCreateUser() throws Exception {
////        UserRequest request = new UserRequest();
////        request.setEmail("test@mail.ru");
////
////        String json = """
////                {
////                "firstName": "Anna",
////                 "lastName": "Vorkuta",
////                  "phone": "+1234567890",
////                   "email": "test@mail.ru"
////                }
////                """;
////        mockMvc.perform(post("/users")
////                        .contentType(MediaType.APPLICATION_JSON)
////                        .content(json))
////                .andExpect(status().isCreated())
////                .andExpect(jsonPath("$.message").value(MessageKeys.USER_CREATED_SUCCESSFULLY));
////        verify(userService).createUser(any(UserRequest.class));
////    }
//
////    @Test
////    void shouldThrowExceptionWhenEmailIsBlank() throws Exception {
////        String json = """
////                {
////                "firstName": "Anna",
////                 "lastName": "Vorkuta",
////                  "phone": "+1234567890",
////                   "email": ""
////                }
////                """;
////
////        mockMvc.perform(post("/users")
////                        .contentType(MediaType.APPLICATION_JSON)
////                        .content(json))
////                .andExpect(status().isBadRequest())
////                .andExpect(jsonPath("$.message").value("Validation failed: Email is required."));
////
////        verify(userService, never()).createUser(any());
////
////    }
//
//    @Test
//    void shouldGetUserById() throws Exception {
//        UUID id = UUID.randomUUID();
//        UserResponse response = new UserResponse();
//        response.setEmail("test@mail.ru");
//
//        when(userService.getByKeycloakId(id)).thenReturn(response);
//
//        mockMvc.perform(get("/users/" + id))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.email").value("test@mail.ru"));
//
//        verify(userService).getByKeycloakId(id);
//    }
//
//    @Test
//    void shouldThrowExceptionWhenUserByIdNotFound() throws Exception {
//        UUID id = UUID.randomUUID();
//
//        when(userService.getByKeycloakId(id)).thenThrow(new ResourceNotFoundException(
//                String.format(MessageKeys.USER_NOT_FOUND, id)
//        ));
//
//        mockMvc.perform(get("/users/" + id))
//                .andExpect(status().isNotFound());
//
//        verify(userService).getByKeycloakId(id);
//
//    }
//
//    @Test
//    void getAllUsers() throws Exception {
//        UserResponse user1 = new UserResponse();
//        user1.setEmail("test1@mail.ru");
//        UserResponse user2 = new UserResponse();
//        user2.setEmail("test2@mail.ru");
//        List<UserResponse> usersList = List.of(user1, user2);
//
//        when(userService.getAllUsers()).thenReturn(usersList);
//
//        mockMvc.perform(get("/users"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.length()").value(2));
//
//        verify(userService).getAllUsers();
//    }
//
//    @Test
//    void shouldUpdateUserById() throws Exception {
//        UUID id = UUID.randomUUID();
//        String json = """
//                {
//                "email": "updated@mail.ru"
//                }""";
//
//
//        mockMvc.perform(put("/users/" + id)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(json))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.message").value(MessageKeys.USER_UPDATE_SUCCESSFULLY));
//
//        verify(userService).updateUserById(any(), any());
//    }
//
//    @Test
//    void shouldDeleteUserById() throws Exception {
//        UUID id = UUID.randomUUID();
//
//        mockMvc.perform(delete("/users/" + id))
//                .andExpect(status().isNoContent());
//
//        verify(userService).deleteUserById(id);
//    }
//}