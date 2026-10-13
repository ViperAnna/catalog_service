package ru.seller_service.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.seller_service.dto.request.application.ReviewSellerApplicationRequest;
import ru.seller_service.exception.GlobalExceptionHandler;
import ru.seller_service.exception.ResourceForbiddenException;
import ru.seller_service.service.StoreChangeModerationService;

import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Очередь модератора заявок на изменение магазина (change store-change-moderation).
 */
class ModeratorStoreChangeControllerTest {

    private StoreChangeModerationService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(StoreChangeModerationService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new ModeratorStoreChangeController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void pending_ok() throws Exception {
        when(service.getPending()).thenReturn(List.of());

        mockMvc.perform(get("/moderator/store-change-requests/pending")).andExpect(status().isOk());
    }

    @Test
    void approve_noContent() throws Exception {
        mockMvc.perform(post("/moderator/store-change-requests/11/approve")).andExpect(status().isNoContent());
        verify(service).approve(11L);
    }

    @Test
    void reject_ok() throws Exception {
        mockMvc.perform(post("/moderator/store-change-requests/11/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"moderatorComment\":\"Invalid phone\"}"))
                .andExpect(status().isOk());
        verify(service).reject(11L, new ReviewSellerApplicationRequest("Invalid phone"));
    }

    @Test
    void reject_withoutReason_badRequest() throws Exception {
        mockMvc.perform(post("/moderator/store-change-requests/11/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Rejection reason is required.")));
        verifyNoInteractions(service);
    }

    @Test
    void approve_ownRequest_forbidden() throws Exception {
        doThrow(new ResourceForbiddenException("Moderator can't review own application."))
                .when(service).approve(11L);

        mockMvc.perform(post("/moderator/store-change-requests/11/approve"))
                .andExpect(status().isForbidden());
    }
}
