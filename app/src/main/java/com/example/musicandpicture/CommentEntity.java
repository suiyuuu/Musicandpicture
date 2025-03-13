package com.example.musicandpicture;

import android.net.Uri;

import java.util.UUID;

/**
 * 评论实体类，存储评论的相关信息
 */
public class CommentEntity {
    private String id;
    private String shareId;      // 关联的分享ID
    private String authorId;     // 评论者ID
    private String authorName;   // 评论者名称
    private String content;      // 评论内容
    private String imagePath;    // 评论图片路径（可选）
    private Uri audioUri;        // 评论音频URI（可选）
    private long timestamp;      // 评论时间戳
    private int likes;           // 点赞数

    /**
     * 创建新评论
     */
    public CommentEntity(String shareId, String authorId, String authorName, String content) {
        this.id = UUID.randomUUID().toString();
        this.shareId = shareId;
        this.authorId = authorId;
        this.authorName = authorName;
        this.content = content;
        this.timestamp = System.currentTimeMillis();
        this.likes = 0;
    }

    /**
     * 完整构造函数
     */
    public CommentEntity(String id, String shareId, String authorId, String authorName,
                         String content, String imagePath, Uri audioUri, long timestamp, int likes) {
        this.id = id;
        this.shareId = shareId;
        this.authorId = authorId;
        this.authorName = authorName;
        this.content = content;
        this.imagePath = imagePath;
        this.audioUri = audioUri;
        this.timestamp = timestamp;
        this.likes = likes;
    }

    // Getter和Setter方法
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getShareId() {
        return shareId;
    }

    public void setShareId(String shareId) {
        this.shareId = shareId;
    }

    public String getAuthorId() {
        return authorId;
    }

    public void setAuthorId(String authorId) {
        this.authorId = authorId;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
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

    public Uri getAudioUri() {
        return audioUri;
    }

    public void setAudioUri(Uri audioUri) {
        this.audioUri = audioUri;
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