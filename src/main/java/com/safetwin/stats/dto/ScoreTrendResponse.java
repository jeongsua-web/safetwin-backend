package com.safetwin.stats.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ScoreTrendResponse {

    private List<String> labels;
    private List<Integer> scores;
    private int target;
    private int min;
    private int current;
}
