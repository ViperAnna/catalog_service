//package ru.klimovich.catalog_service.dto;
//
//import jakarta.validation.constraints.NotBlank;
//import jakarta.validation.constraints.Pattern;
//import jakarta.validation.constraints.Size;
//import lombok.AllArgsConstructor;
//import lombok.Getter;
//import lombok.NoArgsConstructor;
//import lombok.Setter;
//import org.springframework.web.multipart.MultipartFile;
//
//@Getter
//@Setter
//@NoArgsConstructor
//@AllArgsConstructor
//public abstract class TemplateCategory {
//
//    @NotBlank(message = "Category name must not be empty.")
//    private String name;
//    @Size(max = 500, message = "Description can't exceed 500 characters.")
//    private String description;
//    @Pattern(regexp = "ACTIVE|INACTIVE|ARCHIVED|DRAFT", message = "Status must be one of: ACTIVE, INACTIVE, ARCHIVED, DRAFT")
//    private String status;
//}