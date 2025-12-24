package ru.klimovich.catalog_service.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;


@Getter
@AllArgsConstructor
public class Response {
    private String message;
    private int status;
    private LocalDateTime time;
}
