package com.example.musicandpicture;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

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

    // 评论区相关控件
    private RecyclerView commentsRecyclerView;
    private EditText commentEditText;
    private ImageButton sendCommentButton;
    private ImageButton addCommentImageButton;
    private ImageButton addCommentAudioButton;
    private ImageView commentImagePreview;
    private ImageButton cancelImageButton;
    private TextView audioSelectedText;
    private ImageButton cancelAudioButton;

    private String shareId;
    private ShareItem shareItem;
    private MediaPlayer mediaPlayer;

    // 评论相关变量
    private List<CommentEntity> commentList = new ArrayList<>();
    private CommentAdapter commentAdapter;
    private String currentUserId;
    private String currentUserName;
    private Uri selectedImageUri;
    private Uri selectedAudioUri;

    // 用于选择媒体的ActivityResultLauncher
    private androidx.activity.result.ActivityResultLauncher<String> imagePickerLauncher;
    private androidx.activity.result.ActivityResultLauncher<String> audioPickerLauncher;

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

        // 获取用户信息
        setupUserInfo();

        // 注册媒体选择器
        registerMediaPickers();

        // 获取传递的数据
        getIntentData();

        // 加载分享内容
        loadShareContent();

        // 加载评论列表
        loadComments();

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

        // 初始化评论区控件
        commentsRecyclerView = findViewById(R.id.commentsRecyclerView);
        commentEditText = findViewById(R.id.commentEditText);
        sendCommentButton = findViewById(R.id.sendCommentButton);
        addCommentImageButton = findViewById(R.id.addCommentImageButton);
        addCommentAudioButton = findViewById(R.id.addCommentAudioButton);
        commentImagePreview = findViewById(R.id.commentImagePreview);
        cancelImageButton = findViewById(R.id.cancelImageButton);
        audioSelectedText = findViewById(R.id.audioSelectedText);
        cancelAudioButton = findViewById(R.id.cancelAudioButton);
    }

    private void setupUserInfo() {
        // 获取设备ID作为用户ID
        currentUserId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        // 从SharedPreferences获取用户名
        SharedPreferences prefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        currentUserName = prefs.getString("username", "匿名用户");
    }

    private void registerMediaPickers() {
        // 注册图片选择器
        imagePickerLauncher = registerForActivityResult(
                new androidx.activity.result.contract.ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedImageUri = uri;
                        displaySelectedImage();
                    }
                });

        // 注册音频选择器
        audioPickerLauncher = registerForActivityResult(
                new androidx.activity.result.contract.ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedAudioUri = uri;
                        displaySelectedAudio();
                    }
                });
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

    private void loadComments() {
        if (shareId == null) return;

        // 从SharedPreferences加载评论数据
        SharedPreferences prefs = getSharedPreferences("Comments", Context.MODE_PRIVATE);
        String commentsJson = prefs.getString(shareId + "_comments", "[]");

        try {
            JSONArray commentsArray = new JSONArray(commentsJson);
            commentList.clear();

            for (int i = 0; i < commentsArray.length(); i++) {
                JSONObject commentObj = commentsArray.getJSONObject(i);

                String id = commentObj.getString("id");
                String authorId = commentObj.getString("authorId");
                String authorName = commentObj.getString("authorName");
                String content = commentObj.getString("content");
                long timestamp = commentObj.getLong("timestamp");
                int likes = commentObj.getInt("likes");

                CommentEntity comment = new CommentEntity(id, shareId, authorId, authorName,
                        content, null, null, timestamp, likes);

                // 可选字段：图片路径
                if (commentObj.has("imagePath") && !commentObj.isNull("imagePath")) {
                    comment.setImagePath(commentObj.getString("imagePath"));
                }

                // 可选字段：音频URI
                if (commentObj.has("audioUri") && !commentObj.isNull("audioUri")) {
                    comment.setAudioUri(Uri.parse(commentObj.getString("audioUri")));
                }

                commentList.add(comment);
            }

            // 设置适配器
            setupCommentsAdapter();

        } catch (JSONException e) {
            Log.e(TAG, "Error loading comments: " + e.getMessage());
            // 设置空的适配器
            setupCommentsAdapter();
        }
    }

    private void setupCommentsAdapter() {
        // 创建和设置适配器
        commentAdapter = new CommentAdapter(this, commentList);
        commentsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        commentsRecyclerView.setAdapter(commentAdapter);
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

        // 添加评论图片按钮
        addCommentImageButton.setOnClickListener(v -> {
            imagePickerLauncher.launch("image/*");
        });

        // 添加评论音频按钮
        addCommentAudioButton.setOnClickListener(v -> {
            audioPickerLauncher.launch("audio/*");
        });

        // 取消图片按钮
        cancelImageButton.setOnClickListener(v -> {
            selectedImageUri = null;
            commentImagePreview.setVisibility(View.GONE);
            cancelImageButton.setVisibility(View.GONE);
        });

        // 取消音频按钮
        cancelAudioButton.setOnClickListener(v -> {
            selectedAudioUri = null;
            audioSelectedText.setVisibility(View.GONE);
            cancelAudioButton.setVisibility(View.GONE);
        });

        // 发送评论按钮
        sendCommentButton.setOnClickListener(v -> {
            postComment();
        });
    }

    private void displaySelectedImage() {
        if (selectedImageUri != null) {
            // 显示预览
            commentImagePreview.setImageURI(selectedImageUri);
            commentImagePreview.setVisibility(View.VISIBLE);
            cancelImageButton.setVisibility(View.VISIBLE);
        }
    }

    private void displaySelectedAudio() {
        if (selectedAudioUri != null) {
            // 显示所选音频文件名
            String fileName = getFileName(selectedAudioUri);
            audioSelectedText.setText(fileName);
            audioSelectedText.setVisibility(View.VISIBLE);
            cancelAudioButton.setVisibility(View.VISIBLE);
        }
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (nameIndex >= 0) {
                        result = cursor.getString(nameIndex);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (result == null) {
            result = uri.getLastPathSegment();
        }
        return result != null ? result : "未知文件";
    }

    private void postComment() {
        // 获取评论内容
        String commentText = commentEditText.getText().toString().trim();

        // 验证评论内容
        if (commentText.isEmpty() && selectedImageUri == null && selectedAudioUri == null) {
            Toast.makeText(this, "请输入评论内容或添加媒体", Toast.LENGTH_SHORT).show();
            return;
        }

        // 创建评论对象
        CommentEntity comment = new CommentEntity(
                UUID.randomUUID().toString(),
                shareId,
                currentUserId,
                currentUserName,
                commentText,
                null,
                null,
                System.currentTimeMillis(),
                0
        );

        try {
            // 处理图片（如果有）
            if (selectedImageUri != null) {
                Bitmap bitmap = ((BitmapDrawable) commentImagePreview.getDrawable()).getBitmap();
                String imagePath = saveImageToInternalStorage(bitmap);
                comment.setImagePath(imagePath);
            }

            // 处理音频（如果有）
            if (selectedAudioUri != null) {
                comment.setAudioUri(selectedAudioUri);
            }

            // 添加到评论列表
            commentList.add(0, comment);
            if (commentAdapter != null) {
                commentAdapter.notifyItemInserted(0);
                commentsRecyclerView.scrollToPosition(0);
            }

            // 保存评论到SharedPreferences
            saveComments();

            // 清空输入
            commentEditText.setText("");
            selectedImageUri = null;
            selectedAudioUri = null;
            commentImagePreview.setVisibility(View.GONE);
            cancelImageButton.setVisibility(View.GONE);
            audioSelectedText.setVisibility(View.GONE);
            cancelAudioButton.setVisibility(View.GONE);

            Toast.makeText(this, "评论发布成功", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Log.e(TAG, "Error posting comment: " + e.getMessage());
            Toast.makeText(this, "发布评论失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveComments() {
        if (shareId == null) return;

        try {
            // 转换评论列表为JSON数组
            JSONArray commentsArray = new JSONArray();

            for (CommentEntity comment : commentList) {
                JSONObject commentObj = new JSONObject();
                commentObj.put("id", comment.getId());
                commentObj.put("shareId", comment.getShareId());
                commentObj.put("authorId", comment.getAuthorId());
                commentObj.put("authorName", comment.getAuthorName());
                commentObj.put("content", comment.getContent());
                commentObj.put("timestamp", comment.getTimestamp());
                commentObj.put("likes", comment.getLikes());

                // 可选字段
                if (comment.getImagePath() != null) {
                    commentObj.put("imagePath", comment.getImagePath());
                }

                if (comment.getAudioUri() != null) {
                    commentObj.put("audioUri", comment.getAudioUri().toString());
                }

                commentsArray.put(commentObj);
            }

            // 保存到SharedPreferences
            SharedPreferences prefs = getSharedPreferences("Comments", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(shareId + "_comments", commentsArray.toString());
            editor.apply();

        } catch (JSONException e) {
            Log.e(TAG, "Error saving comments: " + e.getMessage());
        }
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

    private String saveImageToInternalStorage(Bitmap bitmap) throws IOException {
        // 创建文件名
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "COMMENT_IMG_" + timeStamp + ".jpg";

        // 获取内部存储目录
        File directory = new File(getFilesDir(), "comment_images");
        if (!directory.exists()) {
            directory.mkdirs();
        }

        // 创建文件
        File imageFile = new File(directory, imageFileName);

        // 保存图片
        FileOutputStream fos = new FileOutputStream(imageFile);
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
        fos.flush();
        fos.close();

        return imageFile.getAbsolutePath();
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

        // 释放评论适配器中的MediaPlayer资源
        if (commentAdapter != null) {
            commentAdapter.releaseMediaPlayer();
        }
    }
}