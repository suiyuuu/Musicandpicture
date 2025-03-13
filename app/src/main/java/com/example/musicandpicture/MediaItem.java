package com.example.musicandpicture;

import android.net.Uri;

public class MediaItem {
    private Uri uri;
    private String fileName;
    private String songName;
    private String artistName;
    private String keywords; // 存储以逗号分隔的关键词

    public MediaItem() {
    }

    public Uri getUri() {
        return uri;
    }

    public void setUri(Uri uri) {
        this.uri = uri;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getSongName() {
        return songName;
    }

    public void setSongName(String songName) {
        this.songName = songName;
    }

    public String getArtistName() {
        return artistName;
    }

    public void setArtistName(String artistName) {
        this.artistName = artistName;
    }

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    /**
     * 检查关键词是否为空
     * @return 如果关键词为空或null则返回true
     */
    public boolean isKeywordsEmpty() {
        return keywords == null || keywords.trim().isEmpty();
    }
}