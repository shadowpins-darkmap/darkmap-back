package com.sp.darkmap.service;

import com.sp.darkmap.converter.ArticleConverter;
import com.sp.darkmap.model.vo.ArticleListResponse;
import com.sp.darkmap.model.vo.ArticleSaveRequest;
import com.sp.darkmap.persistent.entity.Article;
import com.sp.darkmap.persistent.repository.ArticleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
public class ArticleService {
    private final ArticleRepository articleRepository;
    private final ArticleConverter articleConverter;

    @Transactional(readOnly = true)
    public List<ArticleListResponse> getArticlesList() {
        List<Article> articles = articleRepository.findAll();
        return articles.stream().map(articleConverter::toResponse).toList();
    }

    @Transactional
    public void saveArticles(List<ArticleSaveRequest> articleSaveRequestList) {
        List<Article> articles = articleSaveRequestList.stream().map(articleConverter::toEntity).toList();
        articleRepository.saveAll(articles);
    }
}
