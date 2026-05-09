package com.safetwin.repository;

import com.safetwin.entity.Site;
import com.safetwin.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SiteRepository extends JpaRepository<Site, Long> {

    List<Site> findByManager(User manager);

    List<Site> findByManagerId(Long managerId);

    Optional<Site> findByIdAndManagerId(Long id, Long managerId);

    List<Site> findByStatus(Site.Status status);
}
