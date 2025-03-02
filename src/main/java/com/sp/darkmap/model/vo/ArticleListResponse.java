package com.sp.darkmap.model.vo;

import com.sp.darkmap.model.so.Position;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ArticleListResponse {
    private String title;
    private String press;
    private String date;
    private String reporter;
    private String category;
    private String url;
    private String address;
    private String sido;
    private String sigungu;
    private Position position;
}
