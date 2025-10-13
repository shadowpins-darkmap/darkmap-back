package com.sp.community.persistent.repository;

import com.sp.community.persistent.entity.IncidentReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 제보글 Repository
 */
@Repository
public interface IncidentReportRepository extends JpaRepository<IncidentReportEntity, Long> {

}