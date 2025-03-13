package com.example.musicandpicture;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.List;

/**
 * 评论列表适配器
 */
public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    private final Context context;
    private final List<CommentEntity> commentList;
    private final String currentUserId;
    private MediaPlayer mediaPlayer;
    private int playingPosition = -1;

    public CommentAdapter(Context context, List<CommentEntity> commentList) {
        this.context = context;
        this.commentList = commentList;
        // 获取当前用户ID (设备ID)
        this.currentUserId = Settings.Secure.getString(
                context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        CommentEntity comment = commentList.get(position);

        // 设置基本信息
        holder.authorTextView.setText(comment.getAuthorName());
        holder.contentTextView.setText(comment.getContent());
        holder.timeTextView.setText(comment.getFormattedTime());
        holder.likesTextView.setText(String.valueOf(comment.getLikes()));

        // 处理图片（如果有）
        if (comment.getImagePath() != null && !comment.getImagePath().isEmpty()) {
            File imageFile = new File(comment.getImagePath());
            if (imageFile.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(comment.getImagePath());
                holder.commentImageView.setImageBitmap(bitmap);
                holder.commentImageView.setVisibility(View.VISIBLE);

                // 设置图片点击事件，可以放大查看
                holder.commentImageView.setOnClickListener(v -> {
                    // 可以打开全屏查看图片
                    Toast.makeText(context, "查看图片", Toast.LENGTH_SHORT).show();
                    // 这里可以添加打开图片查看器的代码
                });
            } else {
                holder.commentImageView.setVisibility(View.GONE);
            }
        } else {
            holder.commentImageView.setVisibility(View.GONE);
        }

        // 处理音频（如果有）
        if (comment.getAudioUri() != null) {
            holder.audioButton.setVisibility(View.VISIBLE);

            // 根据当前播放状态设置按钮图标
            if (position == playingPosition) {
                holder.audioButton.setImageResource(R.drawable.ic_pause);
            } else {
                holder.audioButton.setImageResource(R.drawable.ic_play);
            }

            // 设置音频播放按钮点击事件
            holder.audioButton.setOnClickListener(v -> {
                if (position == playingPosition && mediaPlayer != null && mediaPlayer.isPlaying()) {
                    // 暂停播放
                    pauseAudio();
                } else {
                    // 播放音频
                    playAudio(comment.getAudioUri(), position, holder.audioButton);
                }
            });
        } else {
            holder.audioButton.setVisibility(View.GONE);
        }

        // 设置点赞按钮
        if (comment.getLikes() > 0) {
            holder.likeImageView.setImageResource(R.drawable.ic_like_filled);
        } else {
            holder.likeImageView.setImageResource(R.drawable.ic_like);
        }

        // 设置点赞按钮点击事件
        holder.likeImageView.setOnClickListener(v -> {
            // 增加点赞数
            comment.setLikes(comment.getLikes() + 1);
            // 更新UI
            holder.likesTextView.setText(String.valueOf(comment.getLikes()));
            holder.likeImageView.setImageResource(R.drawable.ic_like_filled);

            // 显示点赞动画
            Animation animation = AnimationUtils.loadAnimation(context, R.anim.like_animation);
            holder.likeImageView.startAnimation(animation);

            // 这里可以添加保存点赞状态到本地存储的代码
        });

        // 如果是当前用户的评论，显示删除按钮
        if (comment.getAuthorId().equals(currentUserId)) {
            holder.deleteButton.setVisibility(View.VISIBLE);
            holder.deleteButton.setOnClickListener(v -> {
                // 删除评论
                int pos = holder.getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    commentList.remove(pos);
                    notifyItemRemoved(pos);

                    // 这里可以添加从本地存储中删除评论的代码
                    Toast.makeText(context, "评论已删除", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            holder.deleteButton.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return commentList.size();
    }

    /**
     * 播放音频
     */
    private void playAudio(android.net.Uri audioUri, int position, ImageButton button) {
        try {
            // 如果已经有音频在播放，先停止它
            if (mediaPlayer != null) {
                mediaPlayer.release();
            }

            // 创建新的MediaPlayer并播放
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(context, audioUri);
            mediaPlayer.prepare();
            mediaPlayer.start();

            // 更新播放状态
            playingPosition = position;
            button.setImageResource(R.drawable.ic_pause);

            // 设置播放完成监听器
            mediaPlayer.setOnCompletionListener(mp -> {
                button.setImageResource(R.drawable.ic_play);
                playingPosition = -1;
            });
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "播放音频失败", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 暂停音频
     */
    private void pauseAudio() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            notifyItemChanged(playingPosition);
            playingPosition = -1;
        }
    }

    /**
     * 释放MediaPlayer资源
     */
    public void releaseMediaPlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    /**
     * 添加评论
     */
    public void addComment(CommentEntity comment) {
        commentList.add(0, comment);
        notifyItemInserted(0);
    }

    /**
     * 评论ViewHolder
     */
    static class CommentViewHolder extends RecyclerView.ViewHolder {
        TextView authorTextView;
        TextView contentTextView;
        TextView timeTextView;
        TextView likesTextView;
        ImageView commentImageView;
        ImageButton audioButton;
        ImageView likeImageView;
        ImageButton deleteButton;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            authorTextView = itemView.findViewById(R.id.commentAuthorTextView);
            contentTextView = itemView.findViewById(R.id.commentContentTextView);
            timeTextView = itemView.findViewById(R.id.commentTimeTextView);
            likesTextView = itemView.findViewById(R.id.commentLikesTextView);
            commentImageView = itemView.findViewById(R.id.commentImageView);
            audioButton = itemView.findViewById(R.id.commentAudioButton);
            likeImageView = itemView.findViewById(R.id.commentLikeImageView);
            deleteButton = itemView.findViewById(R.id.commentDeleteButton);
        }
    }
}