package com.safetwin.worker.dto;

import lombok.Getter;

import java.time.LocalDate;

@Getter
public class WorkerUpdateRequest {

    private String name;
    private String phone;
    private String occupation;
    private LocalDate startDate;
    private LocalDate endDate;
}
