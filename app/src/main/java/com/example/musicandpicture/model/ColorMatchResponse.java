package com.example.musicandpicture.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * 颜色匹配响应
 */
public class ColorMatchResponse {
    @SerializedName("matched_items")
    private List<MatchResult> matchedItems;

    public List<MatchResult> getMatchedItems() {
        return matchedItems;
    }

    public void setMatchedItems(List<MatchResult> matchedItems) {
        this.matchedItems = matchedItems;
    }
}