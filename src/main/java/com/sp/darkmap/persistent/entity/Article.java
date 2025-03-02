package com.sp.darkmap.persistent.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@Entity
public class Article {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long articleId;

    @Column
    private String title;

    @Column
    private String press;

    @Column
    private String contributionDate;

    @Column
    private String reporter;

    @Column
    private String crimeType;

    @Column
    private String url;

    @Column
    private String address;

    @Column
    private String sido;

    @Column
    private String sigungu;

    @Column
    private Double latitude;

    @Column
    private Double longitude;
}
