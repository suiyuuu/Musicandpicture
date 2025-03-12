package com.example.musicandpicture;

import android.net.Uri;

public class ShareItem {
    private String title;
    private String author;
    private String content;
    private long timestamp;
    private int likes;
    private int coverImageResource; // 用于演示
    private Uri imageUri; // 实际使用中的图片URI
    private Uri audioUri; // 音频URI

    // 构造函数 - 用于演示的带资源ID的构造函数
    public ShareItem(String title, String author, String content, long timestamp, int likes, int coverImageResource) {
        this.title = title;
        this.author = author;
        this.content = content;
        this.timestamp = timestamp;
        this.likes = likes;
        this.coverImageResource = coverImageResource;
    }

    // 带URI的构造函数 - 实际应用中使用
    public ShareItem(String title, String author, String content, long timestamp, int likes, Uri imageUri, Uri audioUri) {
        this.title = title;
        this.author = author;
        this.content = content;
        this.timestamp = timestamp;
        this.likes = likes;
        this.imageUri = imageUri;
        this.audioUri = audioUri;
    }

    // Getters and Setters
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getLikes() {
        return likes;
    }

    public void setLikes(int likes) {
        this.likes = likes;
    }

    public int getCoverImageResource() {
        return coverImageResource;
    }

    public void setCoverImageResource(int coverImageResource) {
        this.coverImageResource = coverImageResource;
    }

    public Uri getImageUri() {
        return imageUri;
    }

    public void setImageUri(Uri imageUri) {
        this.imageUri = imageUri;
    }

    public Uri getAudioUri() {
        return audioUri;
    }

    public void setAudioUri(Uri audioUri) {
        this.audioUri = audioUri;
    }

    // 格式化时间的辅助方法
    public String getFormattedTime() {
        long currentTime = System.currentTimeMillis();
        long diff = currentTime - timestamp;

        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return days + "天前";
        } else if (hours > 0) {
            return hours + "小时前";
        } else if (minutes > 0) {
            return minutes + "分钟前";
        } else {
            return "刚刚";
        }
    }
}