package ru.klimovich.catalog_service.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@NoArgsConstructor

public class CategoryResponse {

    private String id;
    @NotBlank(message = "Category name must not be empty.")
    private String name;
    @Size(max = 500, message = "Description can't exceed 500 characters.")
    private String description;
    @NotBlank
    private String pictureUrl;
    @Pattern(regexp = "ACTIVE|INACTIVE|ARCHIVED|DRAFT", message = "Status must be one of: ACTIVE, INACTIVE, ARCHIVED, DRAFT")
    private String status;
    @JsonFormat(pattern = "dd.MM.yyyy")
    private LocalDateTime createdAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}