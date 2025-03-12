package com.example.musicandpicture;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class CommunityAdapter extends RecyclerView.Adapter<CommunityAdapter.ShareViewHolder> {

    private Context context;
    private List<ShareItem> shareItems;
    private int lastPosition = -1;

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

        // 设置基本信息
        holder.titleTextView.setText(item.getTitle());
        holder.authorTextView.setText(item.getAuthor());
        holder.contentTextView.setText(item.getContent());
        holder.timeTextView.setText(item.getFormattedTime());
        holder.likesTextView.setText(String.valueOf(item.getLikes()));

        // 设置标签
        holder.tagChipGroup.removeAllViews();
        if (item.getTags() != null && !item.getTags().isEmpty()) {
            for (String tag : item.getTags()) {
                Chip chip = new Chip(context);
                chip.setText(tag);
                chip.setChipBackgroundColorResource(R.color.keyword_background);
                chip.setTextColor(context.getResources().getColor(R.color.keyword_text_color));
                chip.setClickable(false);
                holder.tagChipGroup.addView(chip);
            }
            holder.tagChipGroup.setVisibility(View.VISIBLE);
        } else {
            holder.tagChipGroup.setVisibility(View.GONE);
        }

        // 设置图片
        if (item.getImage() != null) {
            // 使用真实的位图
            holder.coverImageView.setImageBitmap(item.getImage());
        } else if (item.getCoverImageResource() > 0) {
            // 使用演示资源ID
            holder.coverImageView.setImageResource(item.getCoverImageResource());
        } else {
            // 使用默认图片
            holder.coverImageView.setImageResource(R.drawable.image_placeholder);
        }

        // 设置卡片点击事件
        holder.cardView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ShareDetailActivity.class);
            intent.putExtra("share_id", item.getId());

            // 如果是演示数据，传递资源ID
            if (item.getId() == null && item.getCoverImageResource() > 0) {
                intent.putExtra("title", item.getTitle());
                intent.putExtra("author", item.getAuthor());
                intent.putExtra("content", item.getContent());
                intent.putExtra("timestamp", item.getTimestamp());
                intent.putExtra("likes", item.getLikes());
                intent.putExtra("imageResourceId", item.getCoverImageResource());
            }

            context.startActivity(intent);
        });

        // 设置点赞按钮点击事件
        holder.likeImageView.setOnClickListener(v -> {
            if (item.getId() != null) {
                toggleLike(item);
                notifyItemChanged(position);
            } else {
                // 演示数据模拟点赞功能
                item.setLikes(item.getLikes() + 1);
                holder.likesTextView.setText(String.valueOf(item.getLikes()));
            }

            // 播放点赞动画
            Animation animation = AnimationUtils.loadAnimation(context, R.anim.like_animation);
            holder.likeImageView.startAnimation(animation);
        });

        // 设置标签状态
        if (item.getLikes() > 0) {
            holder.likeImageView.setImageResource(R.drawable.ic_like_filled);
        } else {
            holder.likeImageView.setImageResource(R.drawable.ic_like);
        }

        // 应用条目动画
        setAnimation(holder.itemView, position);
    }

    private void toggleLike(ShareItem item) {
        // 保存至SharedPreferences
        try {
            SharedPreferences prefs = context.getSharedPreferences("ShareItems", Context.MODE_PRIVATE);
            String itemJson = prefs.getString(item.getId(), null);

            if (itemJson != null) {
                JSONObject itemObject = new JSONObject(itemJson);
                int currentLikes = itemObject.getInt("likes");

                // 增加点赞数
                currentLikes++;
                itemObject.put("likes", currentLikes);

                // 更新内存中的数据
                item.setLikes(currentLikes);

                // 保存回SharedPreferences
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(item.getId(), itemObject.toString());
                editor.apply();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void setAnimation(View viewToAnimate, int position) {
        // 如果当前位置大于上次最后显示的位置，则添加动画
        if (position > lastPosition) {
            Animation animation = AnimationUtils.loadAnimation(context, R.anim.item_animation_from_bottom);
            viewToAnimate.startAnimation(animation);
            lastPosition = position;
        }
    }

    @Override
    public int getItemCount() {
        return shareItems.size();
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull ShareViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        holder.itemView.clearAnimation();
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
        ChipGroup tagChipGroup;

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
            tagChipGroup = itemView.findViewById(R.id.tagChipGroup);
        }
    }
}