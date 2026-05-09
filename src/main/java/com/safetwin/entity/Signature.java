package com.safetwin.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "signatures")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class Signature extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "signer_id")
    private User signer;

    @Column(name = "signer_name", nullable = false, length = 50)
    private String signerName;

    @Column(name = "signature_data", columnDefinition = "TEXT")
    private String signatureData;

    @Column(name = "signed_at")
    private LocalDateTime signedAt;

    @Column(name = "signature_image_url", length = 500)
    private String signatureImageUrl;
}
