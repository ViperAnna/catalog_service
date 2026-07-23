package ru.klimovich.user_service.dto.responce;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponse {

    private String id;

    private String name;

    private BigDecimal price;

    private String articleNumber;

    private List<String> imagesUrl;
}
