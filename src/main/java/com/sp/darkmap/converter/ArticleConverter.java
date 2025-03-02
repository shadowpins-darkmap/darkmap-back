package com.sp.darkmap.converter;

import com.sp.darkmap.model.vo.ArticleListResponse;
import com.sp.darkmap.model.vo.ArticleSaveRequest;
import com.sp.darkmap.persistent.entity.Article;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ArticleConverter {
    @Mapping(target = "crimeType", source = "category")
    @Mapping(target = "contributionDate", source = "date")
    @Mapping(target = "latitude", source = "position.lat")
    @Mapping(target = "longitude", source = "position.lng")
    Article toEntity(ArticleSaveRequest saveRequest);

    @Mapping(target = "category", source = "crimeType")
    @Mapping(target = "date", source = "contributionDate")
    @Mapping(target = "position.lat", source = "latitude")
    @Mapping(target = "position.lng", source = "longitude")
    ArticleListResponse toResponse(Article article);
}
