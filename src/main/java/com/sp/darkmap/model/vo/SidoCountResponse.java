package com.sp.darkmap.model.vo;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class SidoCountResponse {

    private String sido;
    private Long count;

    public SidoCountResponse(String sido, Long count) {
        this.sido = sido;
        this.count = count;
    }
}
