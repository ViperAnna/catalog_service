package ru.klimovich.catalog_service.model;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@ToString
public class Image {
    private String hash;
    private String url;
    private String fileName;
    private Long size;
}
