package com.safetwin.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "analyses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class Analysis extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id", nullable = false)
    private Zone zone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(name = "ai_result", columnDefinition = "TEXT")
    private String aiResult;

    @Column(name = "overall_score")
    private Integer overallScore;

    public enum Status {
        PENDING, IN_PROGRESS, COMPLETED, FAILED
    }

    public void startProcessing() {
        this.status = Status.IN_PROGRESS;
    }

    public void complete(String aiResult, int overallScore) {
        this.status = Status.COMPLETED;
        this.aiResult = aiResult;
        this.overallScore = overallScore;
    }

    public void fail() {
        this.status = Status.FAILED;
    }
}
