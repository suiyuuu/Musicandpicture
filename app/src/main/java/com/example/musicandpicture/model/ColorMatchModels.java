package com.example.musicandpicture.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * 颜色向量
 */
public class ColorVector {
    @SerializedName("values")
    private float[] values;

    public float[] getValues() {
        return values;
    }

    public void setValues(float[] values) {
        this.values = values;
    }
}

/**
 * 音乐项DTO
 */
public class MusicItemDto {
    @SerializedName("id")
    private String id;

    @SerializedName("name")
    private String name;

    @SerializedName("artist")
    private String artist;

    @SerializedName("keywords")
    private String[] keywords;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String[] getKeywords() {
        return keywords;
    }

    public void setKeywords(String[] keywords) {
        this.keywords = keywords;
    }
}

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

/**
 * 颜色匹配请求
 */
public class ColorMatchRequest {
    @SerializedName("color_vector")
    private ColorVector colorVector;

    @SerializedName("music_items")
    private List<MusicItemDto> musicItems;

    public ColorVector getColorVector() {
        return colorVector;
    }

    public void setColorVector(ColorVector colorVector) {
        this.colorVector = colorVector;
    }

    public List<MusicItemDto> getMusicItems() {
        return musicItems;
    }

    public void setMusicItems(List<MusicItemDto> musicItems) {
        this.musicItems = musicItems;
    }
}

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