package com.sp.darkmap.controller;

import com.sp.darkmap.model.vo.ArticleListResponse;
import com.sp.darkmap.model.vo.ArticleSaveRequest;
import com.sp.darkmap.model.vo.SidoCountResponse;
import com.sp.darkmap.service.ArticleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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

    @GetMapping("/articles_")
    public List<ArticleListResponse> getArticlesList_() {
        List<ArticleListResponse> temp0 = articleService.getArticlesList();
        return articleService.getArticlesList();
    }

    // 헤더추가 테스트
    @GetMapping("/articles")
    public ResponseEntity<List<ArticleListResponse>> getArticlesList() {
        List<ArticleListResponse> temp0 = articleService.getArticlesList();
        return ResponseEntity.ok().header("Content-Type", "application/json")
                .header("Cache-Control", "max-age=300, stale-while-revalidate=59")
                .body(temp0);

    }

    @GetMapping("/articles/sido")
    public List<ArticleListResponse> getArticlesBySido(@RequestParam String sido) {
        return articleService.getArticlesBySido(sido);
    }

    @GetMapping("/articles/sido/count")
    public ResponseEntity<List<SidoCountResponse>> getSidoStatistics() {
        List<SidoCountResponse> statistics = articleService.getSidoStatistics();
        return ResponseEntity.ok(statistics);
    }
}
