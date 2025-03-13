package com.example.musicandpicture.model;

import com.google.gson.annotations.SerializedName;

/**
 * 匹配结果
 */
public class MatchResult {
    @SerializedName("music_id")
    private String musicId;

    @SerializedName("match_score")
    private float matchScore;

    public String getMusicId() {
        return musicId;
    }

    public void setMusicId(String musicId) {
        this.musicId = musicId;
    }

    public float getMatchScore() {
        return matchScore;
    }

    public void setMatchScore(float matchScore) {
        this.matchScore = matchScore;
    }
}