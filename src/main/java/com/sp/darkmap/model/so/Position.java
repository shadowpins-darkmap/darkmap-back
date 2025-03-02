package com.sp.darkmap.model.so;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Position {
    @NotNull
    @PositiveOrZero
    private Double lat;
    @NotNull
    @PositiveOrZero
    private Double lng;
}
