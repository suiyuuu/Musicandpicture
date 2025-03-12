package com.example.musicandpicture;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;

import java.util.List;

public class SlideshowAdapter extends RecyclerView.Adapter<SlideshowAdapter.SlideViewHolder> {

    private Context context;
    private List<MediaItem> mediaItems;

    public SlideshowAdapter(Context context, List<MediaItem> mediaItems) {
        this.context = context;
        this.mediaItems = mediaItems;
    }

    @NonNull
    @Override
    public SlideViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_slideshow, parent, false);
        return new SlideViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SlideViewHolder holder, int position) {
        MediaItem item = mediaItems.get(position);

        // 使用Glide加载图片并添加淡入淡出效果
        Glide.with(context)
                .load(item.getUri())
                .transition(DrawableTransitionOptions.withCrossFade(500))
                .centerCrop()
                .error(R.drawable.ic_image_error)
                .into(holder.imageView);
    }

    @Override
    public int getItemCount() {
        return mediaItems.size();
    }

    static class SlideViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public SlideViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.slideshowImageView);
        }
    }
}