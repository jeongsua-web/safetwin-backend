package com.safetwin.publicdata.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class AccidentCaseResponse {

    private String industryType;
    private List<CaseItem> cases;

    @Getter
    @Builder
    public static class CaseItem {
        private String title;
        private int year;
        private String cause;
        private String law;
        private String source;
    }
}
