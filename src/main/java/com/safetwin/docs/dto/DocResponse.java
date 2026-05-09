package com.safetwin.docs.dto;

import com.safetwin.entity.Document;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class DocResponse {

    private Long id;
    private String title;
    private String type;
    private String status;
    private String fileUrl;
    private Long fileSize;
    private Long siteId;
    private String siteName;
    private Long analysisId;
    private LocalDateTime createdAt;

    public static DocResponse from(Document doc) {
        return DocResponse.builder()
                .id(doc.getId())
                .title(doc.getTitle())
                .type(doc.getType().name())
                .status(doc.getStatus().name())
                .fileUrl(doc.getFileUrl())
                .fileSize(doc.getFileSize())
                .siteId(doc.getSite().getId())
                .siteName(doc.getSite().getName())
                .analysisId(doc.getAnalysis() != null ? doc.getAnalysis().getId() : null)
                .createdAt(doc.getCreatedAt())
                .build();
    }
}
