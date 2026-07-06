//package ru.klimovich.user_service.controller;
//
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.HttpStatus;
//import org.springframework.web.bind.annotation.*;
//import ru.klimovich.user_service.dto.Response;
//import ru.klimovich.user_service.dto.request.UserRequest;
//import ru.klimovich.user_service.keycloak.KeycloakUtils;
//
//import java.time.LocalDateTime;
//
//import static ru.klimovich.user_service.util.MessageKeys.USER_CREATED_SUCCESSFULLY;
//
//@RestController
//@RequestMapping("/auth")
//@RequiredArgsConstructor
//public class AuthController {
//
//    private final KeycloakUtils keycloakUtils;
//
//
//    @PostMapping("/register")
//    @ResponseStatus(HttpStatus.CREATED)
//    public Response register(@RequestBody UserRequest userRequest){
//        keycloakUtils.validateUserExists(userRequest);
//        keycloakUtils.createKeycloakUser(userRequest);
//
//        return  new Response(
//                USER_CREATED_SUCCESSFULLY,
//                LocalDateTime.now()
//        );
//    }
//}
