package com.sp.darkmap.persistent.repository;

import com.sp.darkmap.persistent.entity.CrimeCaseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CrimeCaseRepository extends JpaRepository<CrimeCaseEntity, Long> {
}
