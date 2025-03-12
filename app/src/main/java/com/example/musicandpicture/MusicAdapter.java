package com.example.musicandpicture;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MusicAdapter extends RecyclerView.Adapter<MusicAdapter.ViewHolder> {

    private Context context;
    private List<MediaItem> musicItems;

    public MusicAdapter(Context context, List<MediaItem> musicItems) {
        this.context = context;
        this.musicItems = musicItems;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_music, parent, false);
        return new ViewHolder(view);
    }

    // 在MusicAdapter.java的onBindViewHolder方法中修改
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MediaItem item = musicItems.get(position);

        // 设置歌曲名称
        if (item.getSongName() != null && !item.getSongName().isEmpty()) {
            holder.songNameTextView.setText(item.getSongName());
        } else {
            // 如果没有设置歌曲名，使用文件名
            holder.songNameTextView.setText(item.getFileName());
        }

        // 设置艺术家名称
        if (item.getArtistName() != null && !item.getArtistName().isEmpty()) {
            holder.artistNameTextView.setText(item.getArtistName());
        } else {
            holder.artistNameTextView.setText("未知艺术家");
        }

        // 设置点击事件
        holder.itemView.setOnClickListener(v -> {
            // 启动音乐播放器活动
            Intent intent = new Intent(context, MusicPlayerActivity.class);
            intent.putExtra("music_uri", item.getUri());
            intent.putExtra("song_name", item.getSongName() != null ? item.getSongName() : item.getFileName());
            intent.putExtra("artist_name", item.getArtistName() != null ? item.getArtistName() : "未知艺术家");
            intent.putExtra("file_name", item.getFileName()); // 确保传递文件名
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return musicItems.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView songNameTextView;
        TextView artistNameTextView;
        ImageView musicIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            songNameTextView = itemView.findViewById(R.id.songNameTextView);
            artistNameTextView = itemView.findViewById(R.id.artistNameTextView);
            musicIcon = itemView.findViewById(R.id.musicIcon);
        }
    }
}