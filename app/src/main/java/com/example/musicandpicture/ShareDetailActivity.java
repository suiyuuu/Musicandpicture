package com.example.musicandpicture;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ShareDetailActivity extends AppCompatActivity {

    private static final String TAG = "ShareDetailActivity";

    private Toolbar toolbar;
    private ImageView coverImageView;
    private TextView titleTextView;
    private TextView authorTextView;
    private TextView timeTextView;
    private TextView contentTextView;
    private TextView likesTextView;
    private ImageView likeImageView;
    private ChipGroup tagChipGroup;
    private FloatingActionButton musicPlayButton;
    private ImageButton shareButton;

    private String shareId;
    private ShareItem shareItem;
    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share_detail);

        // 初始化视图
        initViews();

        // 设置工具栏
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("");
        }

        // 获取传递的数据
        getIntentData();

        // 加载分享内容
        loadShareContent();

        // 设置监听器
        setupListeners();
    }

    private void initViews() {
        toolbar = findViewById(R.id.detailToolbar);
        coverImageView = findViewById(R.id.detailCoverImageView);
        titleTextView = findViewById(R.id.detailTitleTextView);
        authorTextView = findViewById(R.id.detailAuthorTextView);
        timeTextView = findViewById(R.id.detailTimeTextView);
        contentTextView = findViewById(R.id.detailContentTextView);
        likesTextView = findViewById(R.id.detailLikesTextView);
        likeImageView = findViewById(R.id.detailLikeImageView);
        tagChipGroup = findViewById(R.id.detailTagChipGroup);
        musicPlayButton = findViewById(R.id.musicPlayButton);
        shareButton = findViewById(R.id.detailShareButton);
    }

    private void getIntentData() {
        Intent intent = getIntent();

        // 获取分享ID或演示数据
        if (intent.hasExtra("share_id")) {
            shareId = intent.getStringExtra("share_id");
        } else if (intent.hasExtra("imageResourceId")) {
            // 处理演示数据
            shareItem = new ShareItem();
            shareItem.setTitle(intent.getStringExtra("title"));
            shareItem.setAuthor(intent.getStringExtra("author"));
            shareItem.setContent(intent.getStringExtra("content"));
            shareItem.setTimestamp(intent.getLongExtra("timestamp", System.currentTimeMillis()));
            shareItem.setLikes(intent.getIntExtra("likes", 0));
            shareItem.setCoverImageResource(intent.getIntExtra("imageResourceId", 0));
        }
    }

    private void loadShareContent() {
        if (shareId != null) {
            // 从SharedPreferences加载真实分享数据
            SharedPreferences prefs = getSharedPreferences("ShareItems", Context.MODE_PRIVATE);
            String itemJson = prefs.getString(shareId, null);

            if (itemJson != null) {
                try {
                    JSONObject itemObject = new JSONObject(itemJson);

                    // 创建ShareItem对象
                    shareItem = new ShareItem();
                    shareItem.setId(itemObject.getString("id"));
                    shareItem.setTitle(itemObject.getString("title"));
                    shareItem.setAuthor(itemObject.getString("author"));
                    shareItem.setAuthorId(itemObject.getString("authorId"));
                    shareItem.setContent(itemObject.getString("content"));
                    shareItem.setImagePath(itemObject.getString("imagePath"));
                    shareItem.setTimestamp(itemObject.getLong("timestamp"));
                    shareItem.setLikes(itemObject.getInt("likes"));

                    // 可选字段：音乐URI
                    if (itemObject.has("musicUri")) {
                        shareItem.setAudioUri(Uri.parse(itemObject.getString("musicUri")));
                    }

                    // 读取图片
                    File imageFile = new File(shareItem.getImagePath());
                    if (imageFile.exists()) {
                        Bitmap bitmap = BitmapFactory.decodeFile(shareItem.getImagePath());
                        shareItem.setImage(bitmap);
                    }

                    // 读取标签
                    if (itemObject.has("tags")) {
                        JSONArray tagsArray = itemObject.getJSONArray("tags");
                        List<String> tags = new ArrayList<>();
                        for (int j = 0; j < tagsArray.length(); j++) {
                            tags.add(tagsArray.getString(j));
                        }
                        shareItem.setTags(tags);
                    }

                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing share item JSON: " + e.getMessage());
                    Toast.makeText(this, R.string.error_loading_share, Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
            } else {
                Toast.makeText(this, R.string.share_not_found, Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
        }

        // 更新UI
        updateUI();
    }

    private void updateUI() {
        if (shareItem != null) {
            // 设置标题
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(shareItem.getTitle());
            }

            // 设置基本信息
            titleTextView.setText(shareItem.getTitle());
            authorTextView.setText(shareItem.getAuthor());
            timeTextView.setText(shareItem.getFormattedTime());
            contentTextView.setText(shareItem.getContent());
            likesTextView.setText(String.valueOf(shareItem.getLikes()));

            // 设置点赞图标状态
            if (shareItem.getLikes() > 0) {
                likeImageView.setImageResource(R.drawable.ic_like_filled);
            } else {
                likeImageView.setImageResource(R.drawable.ic_like);
            }

            // 设置图片
            if (shareItem.getImage() != null) {
                coverImageView.setImageBitmap(shareItem.getImage());
            } else if (shareItem.getCoverImageResource() > 0) {
                coverImageView.setImageResource(shareItem.getCoverImageResource());
            } else {
                coverImageView.setImageResource(R.drawable.image_placeholder);
            }

            // 设置标签
            tagChipGroup.removeAllViews();
            if (shareItem.getTags() != null && !shareItem.getTags().isEmpty()) {
                for (String tag : shareItem.getTags()) {
                    Chip chip = new Chip(this);
                    chip.setText(tag);
                    chip.setChipBackgroundColorResource(R.color.keyword_background);
                    chip.setTextColor(getResources().getColor(R.color.keyword_text_color));
                    chip.setClickable(false);
                    tagChipGroup.addView(chip);
                }
                tagChipGroup.setVisibility(View.VISIBLE);
            } else {
                tagChipGroup.setVisibility(View.GONE);
            }

            // 设置音乐按钮状态
            if (shareItem.getAudioUri() != null) {
                musicPlayButton.setVisibility(View.VISIBLE);
            } else {
                musicPlayButton.setVisibility(View.GONE);
            }
        }
    }

    private void setupListeners() {
        // 点赞按钮
        likeImageView.setOnClickListener(v -> {
            if (shareItem != null) {
                toggleLike();
            }
        });

        // 音乐播放按钮
        musicPlayButton.setOnClickListener(v -> {
            if (shareItem != null && shareItem.getAudioUri() != null) {
                toggleMusic();
            }
        });

        // 分享按钮
        shareButton.setOnClickListener(v -> {
            if (shareItem != null) {
                shareContent();
            }
        });
    }

    private void toggleLike() {
        if (shareId != null) {
            // 真实数据点赞
            try {
                SharedPreferences prefs = getSharedPreferences("ShareItems", Context.MODE_PRIVATE);
                String itemJson = prefs.getString(shareId, null);

                if (itemJson != null) {
                    JSONObject itemObject = new JSONObject(itemJson);
                    int currentLikes = itemObject.getInt("likes");

                    // 增加点赞数
                    currentLikes++;
                    itemObject.put("likes", currentLikes);

                    // 更新内存中的数据
                    shareItem.setLikes(currentLikes);

                    // 保存回SharedPreferences
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString(shareId, itemObject.toString());
                    editor.apply();

                    // 更新UI
                    likesTextView.setText(String.valueOf(currentLikes));
                    likeImageView.setImageResource(R.drawable.ic_like_filled);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            // 演示数据点赞
            int likes = shareItem.getLikes() + 1;
            shareItem.setLikes(likes);
            likesTextView.setText(String.valueOf(likes));
            likeImageView.setImageResource(R.drawable.ic_like_filled);
        }
    }

    private void toggleMusic() {
        if (mediaPlayer == null) {
            // 初始化并播放
            try {
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setDataSource(this, shareItem.getAudioUri());
                mediaPlayer.prepare();
                mediaPlayer.start();

                // 更新按钮图标
                musicPlayButton.setImageResource(R.drawable.ic_pause);

                // 设置完成监听
                mediaPlayer.setOnCompletionListener(mp -> {
                    musicPlayButton.setImageResource(R.drawable.ic_play);
                });

            } catch (Exception e) {
                Log.e(TAG, "Error playing music: " + e.getMessage());
                Toast.makeText(this, R.string.error_playing_music, Toast.LENGTH_SHORT).show();
            }
        } else if (mediaPlayer.isPlaying()) {
            // 暂停播放
            mediaPlayer.pause();
            musicPlayButton.setImageResource(R.drawable.ic_play);
        } else {
            // 继续播放
            mediaPlayer.start();
            musicPlayButton.setImageResource(R.drawable.ic_pause);
        }
    }

    private void shareContent() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");

        String shareText = shareItem.getTitle() + "\n\n" +
                shareItem.getContent() + "\n\n" +
                "分享自 " + shareItem.getAuthor() + " - 音乐与图片";

        shareIntent.putExtra(Intent.EXTRA_SUBJECT, shareItem.getTitle());
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);

        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_via)));
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}