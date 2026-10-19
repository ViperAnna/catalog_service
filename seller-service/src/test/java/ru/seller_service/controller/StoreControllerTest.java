package ru.seller_service.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.seller_service.dto.request.store.StoreUpdateRequest;
import ru.seller_service.dto.responce.StoreChangeRequestResponse;
import ru.seller_service.exception.GlobalExceptionHandler;
import ru.seller_service.exception.ResourceConflictException;
import ru.seller_service.model.status.StoreChangeRequestStatus;
import ru.seller_service.service.StoreService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Контракт PUT /stores/{id} после change store-change-moderation: 202 с заявкой.
 */
class StoreControllerTest {

    private StoreService storeService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        storeService = mock(StoreService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new StoreController(storeService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private static StoreChangeRequestResponse pending() {
        StoreChangeRequestResponse r = new StoreChangeRequestResponse();
        r.setId(11L);
        r.setStoreId(3L);
        r.setStoreName("New");
        r.setStatus(StoreChangeRequestStatus.PENDING);
        return r;
    }

    @Test
    void update_accepted_withChangeRequest() throws Exception {
        when(storeService.updateStore(eq(3L), any(StoreUpdateRequest.class))).thenReturn(pending());

        mockMvc.perform(put("/stores/3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"storeName\":\"New\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").value(11))
                .andExpect(jsonPath("$.storeId").value(3))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void update_pendingExists_conflict() throws Exception {
        when(storeService.updateStore(eq(3L), any(StoreUpdateRequest.class)))
                .thenThrow(new ResourceConflictException("Store with id: [3] already has a pending change request."));

        mockMvc.perform(put("/stores/3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"storeName\":\"New\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Store with id: [3] already has a pending change request."));
    }

    @Test
    void changeRequests_list() throws Exception {
        when(storeService.getChangeRequests(3L)).thenReturn(List.of(pending()));

        mockMvc.perform(get("/stores/3/change-requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }
}
