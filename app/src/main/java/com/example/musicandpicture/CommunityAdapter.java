package com.example.musicandpicture;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class CommunityAdapter extends RecyclerView.Adapter<CommunityAdapter.ShareViewHolder> {

    private Context context;
    private List<ShareItem> shareItems;

    public CommunityAdapter(Context context, List<ShareItem> shareItems) {
        this.context = context;
        this.shareItems = shareItems;
    }

    public void updateItems(List<ShareItem> newItems) {
        this.shareItems = newItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ShareViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_share, parent, false);
        return new ShareViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ShareViewHolder holder, int position) {
        ShareItem item = shareItems.get(position);

        holder.titleTextView.setText(item.getTitle());
        holder.authorTextView.setText(item.getAuthor());
        holder.contentTextView.setText(item.getContent());
        holder.timeTextView.setText(item.getFormattedTime());
        holder.likesTextView.setText(String.valueOf(item.getLikes()));

        // 设置图片
        if (item.getImageUri() != null) {
            // 如果有真实的URI，使用它
            Glide.with(context)
                    .load(item.getImageUri())
                    .centerCrop()
                    .placeholder(R.drawable.image_placeholder)
                    .error(R.drawable.ic_image_error)
                    .into(holder.coverImageView);
        } else {
            // 使用演示资源ID
            Glide.with(context)
                    .load(item.getCoverImageResource())
                    .centerCrop()
                    .into(holder.coverImageView);
        }

        // 设置点击事件
        holder.cardView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ShareDetailActivity.class);
            intent.putExtra("title", item.getTitle());
            intent.putExtra("author", item.getAuthor());
            intent.putExtra("content", item.getContent());
            intent.putExtra("timestamp", item.getTimestamp());
            intent.putExtra("likes", item.getLikes());

            // 如果是演示数据，传递资源ID
            if (item.getImageUri() == null) {
                intent.putExtra("imageResourceId", item.getCoverImageResource());
            } else {
                // 传递真实URI
                intent.putExtra("imageUri", item.getImageUri().toString());
                if (item.getAudioUri() != null) {
                    intent.putExtra("audioUri", item.getAudioUri().toString());
                }
            }

            context.startActivity(intent);
        });

        // 点赞按钮
        holder.likeImageView.setOnClickListener(v -> {
            // 模拟点赞功能
            item.setLikes(item.getLikes() + 1);
            holder.likesTextView.setText(String.valueOf(item.getLikes()));
            // 实际应用中，这里应该发送请求到服务器更新点赞数
        });
    }

    @Override
    public int getItemCount() {
        return shareItems.size();
    }

    static class ShareViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        ImageView coverImageView;
        TextView titleTextView;
        TextView authorTextView;
        TextView contentTextView;
        TextView timeTextView;
        TextView likesTextView;
        ImageView likeImageView;

        public ShareViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.shareCardView);
            coverImageView = itemView.findViewById(R.id.coverImageView);
            titleTextView = itemView.findViewById(R.id.titleTextView);
            authorTextView = itemView.findViewById(R.id.authorTextView);
            contentTextView = itemView.findViewById(R.id.contentTextView);
            timeTextView = itemView.findViewById(R.id.timeTextView);
            likesTextView = itemView.findViewById(R.id.likesTextView);
            likeImageView = itemView.findViewById(R.id.likeImageView);
        }
    }
}