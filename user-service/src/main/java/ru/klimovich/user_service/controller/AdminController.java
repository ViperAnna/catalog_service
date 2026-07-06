package ru.klimovich.user_service.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.klimovich.user_service.dto.Response;
import ru.klimovich.user_service.dto.request.UserUpdateRequest;
import ru.klimovich.user_service.dto.responce.UserResponse;
import ru.klimovich.user_service.service.UserService;

import java.time.LocalDateTime;
import java.util.List;

import static ru.klimovich.user_service.util.MessageKeys.USER_DELETE_SUCCESSFULLY;
import static ru.klimovich.user_service.util.MessageKeys.USER_UPDATE_SUCCESSFULLY;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private final UserService userService;
//    private final KeycloakUtils keycloakUtils;


//    @PostMapping("/add")
//    @ResponseStatus(HttpStatus.CREATED)
//    public Response add(@RequestBody UserRequest userRequest) {
//
//        keycloakUtils.createKeycloakUser(userRequest);
//
//        return new Response(
//                USER_CREATED_SUCCESSFULLY,
//                LocalDateTime.now()
//        );
//    }

    @GetMapping("/{keycloakId}")
    public UserResponse getUserById(@PathVariable String keycloakId) {
        return userService.getUserByKeycloakId(keycloakId);
    }

    @GetMapping
    public List<UserResponse> getAllUsers() {
        return userService.getAllUsers();

    }

    @PutMapping("/{keycloakId}")
    public Response updateUserById(@PathVariable String keycloakId, @Valid @ModelAttribute UserUpdateRequest userRequest) {

        userService.updateUserById(keycloakId, userRequest);
        return new Response(
                USER_UPDATE_SUCCESSFULLY,
                LocalDateTime.now()
        );
    }

    @DeleteMapping("/{keycloakId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Response deleteUserById(@PathVariable String keycloakId) {
        userService.deleteUserById(keycloakId);
        return new Response(
                USER_DELETE_SUCCESSFULLY,
                LocalDateTime.now()
        );
    }
}



