package com.example.musicandpicture;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class MediaAdapter extends RecyclerView.Adapter<MediaAdapter.ViewHolder> {

    private Context context;
    private List<MediaItem> mediaItems;
    private boolean isImageAdapter;  // 标识是否是图片适配器

    public MediaAdapter(Context context, List<MediaItem> mediaItems, boolean isImageAdapter) {
        this.context = context;
        this.mediaItems = mediaItems;
        this.isImageAdapter = isImageAdapter;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_image, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MediaItem item = mediaItems.get(position);

        // 使用Glide加载图片
        Glide.with(context)
                .load(item.getUri())
                .centerCrop()
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_image_error)
                .into(holder.imageView);

        // 设置点击事件
        holder.itemView.setOnClickListener(v -> {
            // 点击图片的操作，可以实现预览功能
            // 实现全屏查看图片的功能
        });
    }

    @Override
    public int getItemCount() {
        return mediaItems.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
        }
    }
}