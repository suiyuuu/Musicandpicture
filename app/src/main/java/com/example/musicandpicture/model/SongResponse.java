// SongResponse.java
package com.example.musicandpicture.model;

import java.util.List;

public class SongResponse {
    private int song_id;
    private String song_name;
    private String artist_name;
    private String lyrics;
    private List<String> keywords;
    private String story;

    // Getters and setters

    public int getSongId() {
        return song_id;
    }

    public void setSongId(int song_id) {
        this.song_id = song_id;
    }

    public String getSongName() {
        return song_name;
    }

    public void setSongName(String song_name) {
        this.song_name = song_name;
    }

    public String getArtistName() {
        return artist_name;
    }

    public void setArtistName(String artist_name) {
        this.artist_name = artist_name;
    }

    public String getLyrics() {
        return lyrics;
    }

    public void setLyrics(String lyrics) {
        this.lyrics = lyrics;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    public String getStory() {
        return story;
    }

    public void setStory(String story) {
        this.story = story;
    }
}