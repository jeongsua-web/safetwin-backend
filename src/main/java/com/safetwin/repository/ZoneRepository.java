package com.safetwin.repository;

import com.safetwin.entity.Site;
import com.safetwin.entity.Zone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ZoneRepository extends JpaRepository<Zone, Long> {

    List<Zone> findBySite(Site site);

    List<Zone> findBySiteId(Long siteId);

    @Query("SELECT z FROM Zone z WHERE z.site.id = :siteId AND z.site.manager.id = :managerId")
    List<Zone> findBySiteIdAndManagerId(
            @Param("siteId") Long siteId,
            @Param("managerId") Long managerId);

    @Query("SELECT z FROM Zone z WHERE z.id = :id AND z.site.manager.id = :managerId")
    Optional<Zone> findByIdAndManagerId(@Param("id") Long id, @Param("managerId") Long managerId);
}
