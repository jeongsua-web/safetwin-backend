package com.safetwin.docs.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class SignRequest {

    @NotBlank(message = "서명자 이름은 필수입니다.")
    private String signerName;

    @NotBlank(message = "서명 데이터는 필수입니다.")
    private String signatureData;
}
