package ru.klimovich.user_service.dto.responce;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Ответ на создание вишлиста: прежнее тело {message, time} и id созданного вишлиста.
 */
@Getter
@Setter
@AllArgsConstructor
public class WishlistCreatedResponse {

    private String message;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime time;

    private Long id;
}
