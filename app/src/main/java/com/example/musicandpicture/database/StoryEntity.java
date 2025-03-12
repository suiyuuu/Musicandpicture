package com.example.musicandpicture.database;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "stories")
public class StoryEntity {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String songUri;
    private String keywords;
    private String storyContent;
    private long timestamp;

    public StoryEntity(String songUri, String keywords, String storyContent) {
        this.songUri = songUri;
        this.keywords = keywords;
        this.storyContent = storyContent;
        this.timestamp = System.currentTimeMillis();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getSongUri() {
        return songUri;
    }

    public void setSongUri(String songUri) {
        this.songUri = songUri;
    }

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    public String getStoryContent() {
        return storyContent;
    }

    public void setStoryContent(String storyContent) {
        this.storyContent = storyContent;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}