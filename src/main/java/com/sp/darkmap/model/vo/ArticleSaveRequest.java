package com.sp.darkmap.model.vo;

import com.sp.darkmap.model.so.Position;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ArticleSaveRequest {
    @NotNull
    private String title;
    @NotNull
    private String press;
    @NotNull
    private String date;
    @NotNull
    private String reporter;
    @NotNull
    private String category;
    @NotNull
    private String url;
    @NotNull
    private String address;
    @NotNull
    private String sido;
    @NotNull
    private String sigungu;
    private Position position;
}
