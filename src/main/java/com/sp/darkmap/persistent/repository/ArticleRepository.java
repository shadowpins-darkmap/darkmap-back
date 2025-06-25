package com.sp.darkmap.persistent.repository;

import com.sp.darkmap.persistent.entity.Article;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {
    List<Article> findBySido(String sido);
}
