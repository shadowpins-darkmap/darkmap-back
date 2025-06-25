package com.sp.darkmap.controller;

import com.sp.darkmap.model.vo.ArticleListResponse;
import com.sp.darkmap.model.vo.ArticleSaveRequest;
import com.sp.darkmap.service.ArticleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
public class ArticleController {
    private final ArticleService articleService;

    @PostMapping("/articles")
    public String saveArticles(@Valid @RequestBody List<ArticleSaveRequest> saveRequest) {
        articleService.saveArticles(saveRequest);
        return "ok!";
    }

    @GetMapping("/articles")
    public List<ArticleListResponse> getArticlesList() {
        return articleService.getArticlesList();
    }

    @GetMapping("/articles/sido")
    public List<ArticleListResponse> getArticlesBySido(@RequestParam String sido) {
        return articleService.getArticlesBySido(sido);
    }
}
