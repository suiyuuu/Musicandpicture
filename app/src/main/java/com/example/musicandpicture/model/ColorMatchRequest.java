package com.example.musicandpicture.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

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