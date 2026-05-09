package com.safetwin.repository;

import com.safetwin.entity.Signature;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SignatureRepository extends JpaRepository<Signature, Long> {

    List<Signature> findByDocumentId(Long documentId);

    Optional<Signature> findByDocumentIdAndSignerId(Long documentId, Long signerId);

    boolean existsByDocumentIdAndSignerId(Long documentId, Long signerId);
}
