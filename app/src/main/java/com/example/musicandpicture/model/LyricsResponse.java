package com.example.musicandpicture.model;

import com.google.gson.annotations.SerializedName;

public class LyricsResponse {

    @SerializedName("lyrics")
    private String lyrics;

    @SerializedName("artist")
    private String artist;

    @SerializedName("song")
    private String song;

    public String getLyrics() {
        return lyrics;
    }

    public String getArtist() {
        return artist;
    }

    public String getSong() {
        return song;
    }
}
