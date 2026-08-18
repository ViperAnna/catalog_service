package ru.seller_service.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class ModeratorRequest {

    private boolean approved;
    private String mediatorComment;
}
