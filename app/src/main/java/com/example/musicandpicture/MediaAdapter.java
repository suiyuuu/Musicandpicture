package com.example.musicandpicture;

import android.content.Context;
import android.content.Intent;
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
    private boolean isImageAdapter;  // Flag to identify if this is an image adapter

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

        // Load image using Glide
        Glide.with(context)
                .load(item.getUri())
                .centerCrop()
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_image_error)
                .into(holder.imageView);

        // Set click event for the image
        holder.itemView.setOnClickListener(v -> {
            // Open fullscreen view when image is clicked
            if (isImageAdapter) {
                Intent intent = new Intent(context, ImageFullscreenActivity.class);
                intent.putExtra("position", position);
                context.startActivity(intent);
            }
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