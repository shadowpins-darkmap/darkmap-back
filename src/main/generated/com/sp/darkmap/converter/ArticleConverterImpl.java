package com.sp.darkmap.converter;

import com.sp.darkmap.model.so.Position;
import com.sp.darkmap.model.vo.ArticleListResponse;
import com.sp.darkmap.model.vo.ArticleSaveRequest;
import com.sp.darkmap.persistent.entity.Article;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2024-11-30T02:28:13+0900",
    comments = "version: 1.6.3, compiler: javac, environment: Java 17.0.10 (Azul Systems, Inc.)"
)
@Component
public class ArticleConverterImpl implements ArticleConverter {

    @Override
    public Article toEntity(ArticleSaveRequest saveRequest) {
        if ( saveRequest == null ) {
            return null;
        }

        Article article = new Article();

        article.setCrimeType( saveRequest.getCategory() );
        article.setContributionDate( saveRequest.getDate() );
        article.setLatitude( saveRequestPositionLat( saveRequest ) );
        article.setLongitude( saveRequestPositionLng( saveRequest ) );
        article.setTitle( saveRequest.getTitle() );
        article.setPress( saveRequest.getPress() );
        article.setReporter( saveRequest.getReporter() );
        article.setUrl( saveRequest.getUrl() );
        article.setAddress( saveRequest.getAddress() );
        article.setSido( saveRequest.getSido() );
        article.setSigungu( saveRequest.getSigungu() );

        return article;
    }

    @Override
    public ArticleListResponse toResponse(Article article) {
        if ( article == null ) {
            return null;
        }

        ArticleListResponse articleListResponse = new ArticleListResponse();

        articleListResponse.setPosition( articleToPosition( article ) );
        articleListResponse.setCategory( article.getCrimeType() );
        articleListResponse.setDate( article.getContributionDate() );
        articleListResponse.setTitle( article.getTitle() );
        articleListResponse.setPress( article.getPress() );
        articleListResponse.setReporter( article.getReporter() );
        articleListResponse.setUrl( article.getUrl() );
        articleListResponse.setAddress( article.getAddress() );
        articleListResponse.setSido( article.getSido() );
        articleListResponse.setSigungu( article.getSigungu() );

        return articleListResponse;
    }

    private Double saveRequestPositionLat(ArticleSaveRequest articleSaveRequest) {
        Position position = articleSaveRequest.getPosition();
        if ( position == null ) {
            return null;
        }
        return position.getLat();
    }

    private Double saveRequestPositionLng(ArticleSaveRequest articleSaveRequest) {
        Position position = articleSaveRequest.getPosition();
        if ( position == null ) {
            return null;
        }
        return position.getLng();
    }

    protected Position articleToPosition(Article article) {
        if ( article == null ) {
            return null;
        }

        Position position = new Position();

        position.setLat( article.getLatitude() );
        position.setLng( article.getLongitude() );

        return position;
    }
}
