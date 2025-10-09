package com.sp.darkmap.persistent.repository;

import com.sp.darkmap.model.vo.SidoCountResponse;
import com.sp.darkmap.persistent.entity.Article;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {
    List<Article> findBySido(String sido);

    @Query("SELECT new com.sp.darkmap.model.vo.SidoCountResponse(a.sido, COUNT(a)) " +
            "FROM Article a " +
            "WHERE a.sido IS NOT NULL " +
            "GROUP BY a.sido " +
            "ORDER BY COUNT(a) DESC")
    List<SidoCountResponse> countBySido();

    /**
     * 전체검색용
     * @param keyword
     * @return
     */
    @Query("SELECT a FROM Article a WHERE a.title LIKE %:keyword%")
    List<Article> searchByKeyword(@Param("keyword") String keyword);
}
