package ru.klimovich.catalog_service.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import ru.klimovich.catalog_service.dto.TemplateCategory;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@NoArgsConstructor

public class CategoryResponse extends TemplateCategory {

    private String id;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dateCreate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dateUpdate;
}