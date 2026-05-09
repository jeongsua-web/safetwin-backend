package com.safetwin.stats.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CompareResponse {

    private Long beforeId;
    private Long afterId;
    private int beforeScore;
    private int afterScore;
    private int scoreDelta;
    private int beforeRiskCount;
    private int afterRiskCount;
    private int riskDelta;
    private List<String> improvedItems;
    private List<String> newItems;
    private List<String> persistedItems;
}
