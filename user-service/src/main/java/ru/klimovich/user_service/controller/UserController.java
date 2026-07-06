package ru.klimovich.user_service.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import ru.klimovich.user_service.dto.Response;
import ru.klimovich.user_service.dto.request.UserUpdateRequest;
import ru.klimovich.user_service.dto.responce.UserResponse;
import ru.klimovich.user_service.service.UserService;

import java.time.LocalDateTime;

import static ru.klimovich.user_service.util.MessageKeys.USER_DELETE_SUCCESSFULLY;
import static ru.klimovich.user_service.util.MessageKeys.USER_UPDATE_SUCCESSFULLY;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
public class UserController {
    private final UserService userService;

    @GetMapping("/me")
    public UserResponse getMyAccount(@AuthenticationPrincipal Jwt jwt) {
        return userService.getOrCreateUser(jwt);
    }

    @PutMapping("/me")
    public Response updateMyAccount(@AuthenticationPrincipal Jwt jwt, @Valid @ModelAttribute UserUpdateRequest userRequest) {

        userService.updateUserById(jwt.getSubject(), userRequest);
        return new Response(
                USER_UPDATE_SUCCESSFULLY,
                LocalDateTime.now()
        );
    }

    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Response deleteMyAccount(@AuthenticationPrincipal Jwt jwt) {
        userService.deleteUserById(jwt.getSubject());
        return new Response(
                USER_DELETE_SUCCESSFULLY,
                LocalDateTime.now()
        );
    }
}



