package ru.seller_service.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.seller_service.controller.ModeratorController;
import ru.seller_service.controller.SellerApplicationController;
import ru.seller_service.controller.StoreController;
import ru.seller_service.dto.request.application.ReviewSellerApplicationRequest;
import ru.seller_service.service.ModeratorService;
import ru.seller_service.service.SellerApplicationService;
import ru.seller_service.service.StoreService;
import ru.seller_service.util.MessageKeys;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Формат ошибок seller-service (change seller-service-error-handling):
 * тело {message, time} для 400, 404, 409, 500; 401/403 Spring Security не перехватываются.
 */
class GlobalExceptionHandlerTest {

    private static final String TIME_PATTERN = "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}";

    private StoreService storeService;
    private ModeratorService moderatorService;
    private SellerApplicationService sellerApplicationService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        storeService = mock(StoreService.class);
        moderatorService = mock(ModeratorService.class);
        sellerApplicationService = mock(SellerApplicationService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(
                        new StoreController(storeService),
                        new ModeratorController(moderatorService),
                        new SellerApplicationController(sellerApplicationService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("400: ошибка валидации @RequestBody дает Validation failed с сообщением поля")
    void requestBodyValidation_badRequest() throws Exception {
        mockMvc.perform(post("/stores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"storeName\":\"Shop\",\"email\":\"shop@example.com\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", startsWith("Validation failed: ")))
                .andExpect(jsonPath("$.message", containsString("Phone is required.")))
                .andExpect(jsonPath("$.time", matchesPattern(TIME_PATTERN)));
        verifyNoInteractions(storeService);
    }

    @Test
    @DisplayName("400: ошибка валидации @ModelAttribute (PUT /seller-applications/update)")
    void modelAttributeValidation_badRequest() throws Exception {
        mockMvc.perform(put("/seller-applications/update")
                        .param("phoneNumber", "+79991111111")
                        .param("paymentAccount", "1234567890123456789"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", startsWith("Validation failed: ")))
                .andExpect(jsonPath("$.message", containsString("Payment account must contain exactly 20 digits.")))
                .andExpect(jsonPath("$.message", not(containsString("Phone"))));
        verifyNoInteractions(sellerApplicationService);
    }

    @Test
    @DisplayName("400: форма UPDATE со старым параметром phone вместо phoneNumber")
    void modelAttribute_oldPhoneParam_badRequest() throws Exception {
        mockMvc.perform(put("/seller-applications/update")
                        .param("phone", "+79991111111")
                        .param("paymentAccount", "12345678901234567890"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Phone is required.")));
        verifyNoInteractions(sellerApplicationService);
    }

    @Test
    @DisplayName("400: нечитаемое тело reject (обрезанный JSON)")
    void unreadableBody_badRequest() throws Exception {
        mockMvc.perform(post("/moderator/seller-applications/5/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"moderatorComment\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.message", not(containsString("ru.seller_service"))))
                .andExpect(jsonPath("$.time", matchesPattern(TIME_PATTERN)));
        verifyNoInteractions(moderatorService);
    }

    @Test
    @DisplayName("400: нечисловой id в пути")
    void typeMismatch_badRequest() throws Exception {
        mockMvc.perform(get("/moderator/seller-applications/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.time", matchesPattern(TIME_PATTERN)));
        verifyNoInteractions(moderatorService);
    }

    @Test
    @DisplayName("404: ResourceNotFoundException с текстом исключения")
    void notFound() throws Exception {
        when(moderatorService.getApplication(999999L))
                .thenThrow(new ResourceNotFoundException(MessageKeys.APPLICATION_NOT_FOUND, "999999"));

        mockMvc.perform(get("/moderator/seller-applications/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Application not found with id: [999999]."))
                .andExpect(jsonPath("$.time", matchesPattern(TIME_PATTERN)));
    }

    @Test
    @DisplayName("404: магазин не найден")
    void storeNotFound() throws Exception {
        when(storeService.getStoreById(999999L))
                .thenThrow(new ResourceNotFoundException(MessageKeys.STORE_NOT_FOUND, "999999"));

        mockMvc.perform(get("/stores/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Store not found with id: [999999]."));
    }

    @Test
    @DisplayName("409: ResourceConflictException с текстом исключения")
    void conflict() throws Exception {
        doThrow(new ResourceConflictException(MessageKeys.APPLICATION_ALREADY_REVIEWED, 5L))
                .when(moderatorService).approve(5L);

        mockMvc.perform(post("/moderator/seller-applications/5/approve"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Application with id: [5] already reviewed."))
                .andExpect(jsonPath("$.time", matchesPattern(TIME_PATTERN)));
    }

    @Test
    @DisplayName("500: прочее исключение без текста и деталей")
    void unexpected_internalServerError() throws Exception {
        doThrow(new IllegalStateException("secret db detail"))
                .when(moderatorService).approve(anyLong());

        mockMvc.perform(post("/moderator/seller-applications/5/approve"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Internal server error"))
                .andExpect(jsonPath("$.time", matchesPattern(TIME_PATTERN)));
    }

    @Test
    @DisplayName("AccessDeniedException не перехватывается и остается для Spring Security")
    void accessDenied_rethrown() {
        when(storeService.getMyStores()).thenThrow(new AccessDeniedException("Access Denied"));

        assertThatThrownBy(() -> mockMvc.perform(get("/stores")))
                .hasRootCauseInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("409 для занятого имени магазина в теле {message, time}")
    void storeNameConflict() throws Exception {
        when(storeService.createStore(any()))
                .thenThrow(new ResourceConflictException(MessageKeys.STORE_ALREADY_EXISTS, "shop"));

        mockMvc.perform(post("/stores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"storeName\":\"shop\",\"email\":\"shop@example.com\",\"phoneNumber\":\"+79990000000\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Store with name [shop] already exists."));
    }
    // --- Правила модерации (change seller-moderation-rules) ---

    @Test
    @DisplayName("403: ResourceForbiddenException в теле {message, time}")
    void forbidden() throws Exception {
        doThrow(new ResourceForbiddenException(MessageKeys.OWN_APPLICATION_REVIEW_FORBIDDEN))
                .when(moderatorService).approve(5L);

        mockMvc.perform(post("/moderator/seller-applications/5/approve"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Moderator can't review own application."))
                .andExpect(jsonPath("$.time", matchesPattern(TIME_PATTERN)));
    }

    @Test
    @DisplayName("400: reject без причины отказа, сервис не вызывается")
    void reject_withoutReason_badRequest() throws Exception {
        mockMvc.perform(post("/moderator/seller-applications/5/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Rejection reason is required.")));
        verifyNoInteractions(moderatorService);
    }

    @Test
    @DisplayName("200: reject со старым полем status = APPROVED, поле игнорируется")
    void reject_withLegacyStatus_ok() throws Exception {
        mockMvc.perform(post("/moderator/seller-applications/5/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APPROVED\",\"moderatorComment\":\"Invalid INN\"}"))
                .andExpect(status().isOk());
        verify(moderatorService).reject(eq(5L), eq(new ReviewSellerApplicationRequest("Invalid INN")));
    }

    @Test
    @DisplayName("200: reject только с причиной, без status")
    void reject_withoutStatus_ok() throws Exception {
        mockMvc.perform(post("/moderator/seller-applications/5/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"moderatorComment\":\"Invalid INN\"}"))
                .andExpect(status().isOk());
        verify(moderatorService).reject(eq(5L), eq(new ReviewSellerApplicationRequest("Invalid INN")));
    }
    @Test
    @DisplayName("400: заявка UNBLOCK без причины, сервис не вызывается")
    void unblockApplication_withoutReason_badRequest() throws Exception {
        mockMvc.perform(post("/seller-applications/unblock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", startsWith("Validation failed: ")));
        verifyNoInteractions(sellerApplicationService);
    }
}
