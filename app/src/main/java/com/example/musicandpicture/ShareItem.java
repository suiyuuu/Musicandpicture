package com.example.musicandpicture;

import android.graphics.Bitmap;
import android.net.Uri;

import java.util.ArrayList;
import java.util.List;

/**
 * 改进版的分享项类，支持本地存储和更多社区功能
 */
public class ShareItem {
    private String id;
    private String title;
    private String author;
    private String authorId;
    private String content;
    private String imagePath;
    private Bitmap image;
    private long timestamp;
    private int likes;
    private Uri audioUri;
    private List<String> tags;

    // Demo用的资源ID字段
    private int coverImageResource;

    /**
     * 默认构造函数
     */
    public ShareItem() {
        this.tags = new ArrayList<>();
    }

    /**
     * 演示数据构造函数
     */
    public ShareItem(String title, String author, String content, long timestamp, int likes, int coverImageResource) {
        this.title = title;
        this.author = author;
        this.content = content;
        this.timestamp = timestamp;
        this.likes = likes;
        this.coverImageResource = coverImageResource;
        this.tags = new ArrayList<>();
    }

    /**
     * 完整构造函数
     */
    public ShareItem(String id, String title, String author, String authorId, String content,
                     String imagePath, Bitmap image, long timestamp, int likes, Uri audioUri) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.authorId = authorId;
        this.content = content;
        this.imagePath = imagePath;
        this.image = image;
        this.timestamp = timestamp;
        this.likes = likes;
        this.audioUri = audioUri;
        this.tags = new ArrayList<>();
    }

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

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

    public String getAuthorId() {
        return authorId;
    }

    public void setAuthorId(String authorId) {
        this.authorId = authorId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public Bitmap getImage() {
        return image;
    }

    public void setImage(Bitmap image) {
        this.image = image;
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

    public Uri getAudioUri() {
        return audioUri;
    }

    public void setAudioUri(Uri audioUri) {
        this.audioUri = audioUri;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public void addTag(String tag) {
        if (this.tags == null) {
            this.tags = new ArrayList<>();
        }
        this.tags.add(tag);
    }

    /**
     * 格式化时间的辅助方法
     */
    public String getFormattedTime() {
        long currentTime = System.currentTimeMillis();
        long diff = currentTime - timestamp;

        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 30) {
            return days / 30 + "个月前";
        } else if (days > 0) {
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