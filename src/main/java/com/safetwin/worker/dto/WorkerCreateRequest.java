package com.safetwin.worker.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

import java.time.LocalDate;

@Getter
public class WorkerCreateRequest {

    @NotBlank(message = "근로자 이름은 필수입니다.")
    private String name;

    private String phone;
    private String occupation;
    private LocalDate startDate;
    private LocalDate endDate;
}
