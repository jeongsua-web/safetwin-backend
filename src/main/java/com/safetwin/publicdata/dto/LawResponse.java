package com.safetwin.publicdata.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class LawResponse {

    private String keyword;
    private List<LawItem> laws;

    @Getter
    @Builder
    public static class LawItem {
        private String title;
        private String article;
        private String content;
        private String category;
    }
}
