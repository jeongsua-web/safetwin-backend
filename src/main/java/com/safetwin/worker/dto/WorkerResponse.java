package com.safetwin.worker.dto;

import com.safetwin.entity.Worker;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class WorkerResponse {

    private Long id;
    private Long siteId;
    private String siteName;
    private String name;
    private String phone;
    private String occupation;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDateTime createdAt;

    public static WorkerResponse from(Worker worker) {
        return WorkerResponse.builder()
                .id(worker.getId())
                .siteId(worker.getSite().getId())
                .siteName(worker.getSite().getName())
                .name(worker.getName())
                .phone(worker.getPhone())
                .occupation(worker.getOccupation())
                .startDate(worker.getStartDate())
                .endDate(worker.getEndDate())
                .createdAt(worker.getCreatedAt())
                .build();
    }
}
