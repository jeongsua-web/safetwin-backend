package com.safetwin.docs.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.util.List;

@Getter
public class EducationCertRequest {

    @NotNull(message = "사업장 ID는 필수입니다.")
    private Long siteId;

    @NotBlank(message = "교육 일시는 필수입니다.")
    private String educationDate;

    @NotBlank(message = "교육 내용은 필수입니다.")
    private String content;

    private List<Long> workerIds;
}
