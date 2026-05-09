package com.safetwin.repository;

import com.safetwin.entity.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    @Query("""
            SELECT d FROM Document d JOIN d.site s
            WHERE s.manager.id = :managerId
            AND (:type IS NULL OR d.type = :type)
            AND (:status IS NULL OR d.status = :status)
            ORDER BY d.createdAt DESC
            """)
    Page<Document> findByManagerIdWithFilters(
            @Param("managerId") Long managerId,
            @Param("type") Document.DocumentType type,
            @Param("status") Document.DocStatus status,
            Pageable pageable);

    @Query("SELECT d FROM Document d JOIN d.site s WHERE d.id = :id AND s.manager.id = :managerId")
    Optional<Document> findByIdAndManagerId(@Param("id") Long id, @Param("managerId") Long managerId);

    @Query("SELECT COUNT(d) FROM Document d JOIN d.site s WHERE s.manager.id = :managerId AND d.type = :type")
    long countByManagerIdAndType(@Param("managerId") Long managerId, @Param("type") Document.DocumentType type);

    @Query("SELECT COUNT(d) FROM Document d JOIN d.site s WHERE s.manager.id = :managerId AND d.type = :type AND d.status = :status")
    long countByManagerIdAndTypeAndStatus(
            @Param("managerId") Long managerId,
            @Param("type") Document.DocumentType type,
            @Param("status") Document.DocStatus status);

    List<Document> findBySiteId(Long siteId);

    List<Document> findBySiteIdAndType(Long siteId, Document.DocumentType type);

    List<Document> findByUploaderId(Long uploaderId);
}
