package ru.seller_service.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.seller_service.exception.GlobalExceptionHandler;
import ru.seller_service.exception.ResourceConflictException;
import ru.seller_service.exception.ResourceNotFoundException;
import ru.seller_service.service.SellerModerationService;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Эндпоинты блокировки модератора (change seller-store-lifecycle).
 */
class ModeratorBlockControllerTest {

    private SellerModerationService moderationService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        moderationService = mock(SellerModerationService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new ModeratorBlockController(moderationService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void blockSeller_noContent() throws Exception {
        mockMvc.perform(post("/moderator/sellers/7/block")).andExpect(status().isNoContent());
        verify(moderationService).blockSeller(7L);
    }

    @Test
    void unblockSeller_noContent() throws Exception {
        mockMvc.perform(post("/moderator/sellers/7/unblock")).andExpect(status().isNoContent());
        verify(moderationService).unblockSeller(7L);
    }

    @Test
    void blockStore_noContent() throws Exception {
        mockMvc.perform(post("/moderator/stores/3/block")).andExpect(status().isNoContent());
        verify(moderationService).blockStore(3L);
    }

    @Test
    void unblockStore_noContent() throws Exception {
        mockMvc.perform(post("/moderator/stores/3/unblock")).andExpect(status().isNoContent());
        verify(moderationService).unblockStore(3L);
    }

    @Test
    void blockSeller_notFound() throws Exception {
        doThrow(new ResourceNotFoundException("Seller not found with id: [99]."))
                .when(moderationService).blockSeller(99L);

        mockMvc.perform(post("/moderator/sellers/99/block"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Seller not found with id: [99]."));
    }

    @Test
    void unblockStore_conflict() throws Exception {
        doThrow(new ResourceConflictException("Store with id: [3] has status [ACTIVE]."))
                .when(moderationService).unblockStore(3L);

        mockMvc.perform(post("/moderator/stores/3/unblock"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Store with id: [3] has status [ACTIVE]."));
    }
}
