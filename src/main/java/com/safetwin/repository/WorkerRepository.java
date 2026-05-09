package com.safetwin.repository;

import com.safetwin.entity.Worker;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkerRepository extends JpaRepository<Worker, Long> {

    List<Worker> findBySiteId(Long siteId);

    List<Worker> findByUserId(Long userId);

    boolean existsBySiteIdAndUserId(Long siteId, Long userId);

    Optional<Worker> findByIdAndSiteManagerId(Long id, Long managerId);

    List<Worker> findBySiteIdAndSiteManagerId(Long siteId, Long managerId);
}
