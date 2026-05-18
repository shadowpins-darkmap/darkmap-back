package com.sp.cyberflashing.persistent.repository;

import com.sp.cyberflashing.persistent.entity.CyberFlashingCaseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CyberFlashingCaseRepository extends JpaRepository<CyberFlashingCaseEntity, Long> {

    @Query("""
            SELECT c
            FROM CyberFlashingCaseEntity c
            WHERE (:countryCode IS NULL OR c.countryCode = :countryCode)
              AND (:duplicateFlag IS NULL OR c.duplicateFlag = :duplicateFlag)
              AND (:includeFlag IS NULL OR c.includeFlag = :includeFlag)
            """)
    Page<CyberFlashingCaseEntity> search(
            @Param("countryCode") String countryCode,
            @Param("duplicateFlag") String duplicateFlag,
            @Param("includeFlag") String includeFlag,
            Pageable pageable
    );

    @Query("""
            SELECT COUNT(c)
            FROM CyberFlashingCaseEntity c
            WHERE (:countryCode IS NULL OR c.countryCode = :countryCode)
              AND (:duplicateFlag IS NULL OR c.duplicateFlag = :duplicateFlag)
              AND (:includeFlag IS NULL OR c.includeFlag = :includeFlag)
            """)
    long countByFilters(
            @Param("countryCode") String countryCode,
            @Param("duplicateFlag") String duplicateFlag,
            @Param("includeFlag") String includeFlag
    );
}
