package ru.klimovich.catalog_service.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class CategoryResponseDTO {

    private String id;
    private String name;
    private String description;
    private String picture;
    private String status;
    private LocalDateTime dateCreate;
    private LocalDateTime dateUpdate;

}
