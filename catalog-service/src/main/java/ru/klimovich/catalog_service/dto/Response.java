package ru.klimovich.catalog_service.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@Schema(description = "Формирование ответа")
public class Response {

    @Schema(description = "Сообщение ответа", example = "Сообщение ответа:Успешно/не успешно")
    private String message;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "Время ответа", example = "2026-02-11 00:00:01")
    private LocalDateTime time;
}