package com.example.musicandpicture;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class ShareContentActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private EditText titleEditText;
    private EditText contentEditText;
    private ImageView selectedImageView;
    private TextView selectImageTextView;
    private TextView selectMusicTextView;
    private ChipGroup tagChipGroup;
    private Button shareButton;
    private ProgressBar progressBar;

    private Uri selectedImageUri;
    private Uri selectedMusicUri;
    private String deviceId;
    private String authorName;

    // Activity Result Launchers
    private final ActivityResultLauncher<String> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    loadImage();
                }
            });

    private final ActivityResultLauncher<String> musicPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedMusicUri = uri;
                    updateMusicSelection();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share_content);

        // 初始化视图
        initViews();

        // 设置工具栏
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.share_content);
        }

        // 获取作者ID和名称（使用设备ID作为匿名ID）
        setupAuthorInfo();

        // 设置标签
        setupTags();

        // 设置监听器
        setupListeners();

        // 检查是否从其他活动传递了数据
        checkIntentData();
    }

    private void initViews() {
        toolbar = findViewById(R.id.shareToolbar);
        titleEditText = findViewById(R.id.shareTitleEditText);
        contentEditText = findViewById(R.id.shareContentEditText);
        selectedImageView = findViewById(R.id.selectedImageView);
        selectImageTextView = findViewById(R.id.selectImageTextView);
        selectMusicTextView = findViewById(R.id.selectMusicTextView);
        tagChipGroup = findViewById(R.id.tagChipGroup);
        shareButton = findViewById(R.id.shareButton);
        progressBar = findViewById(R.id.shareProgressBar);
    }

    private void setupAuthorInfo() {
        // 使用设备ID作为匿名标识符
        deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        // 从SharedPreferences获取作者名称，如果没有则使用"匿名用户"
        SharedPreferences prefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        authorName = prefs.getString("username", "匿名用户");

        // 如果是第一次，弹出对话框让用户输入昵称
        if (authorName.equals("匿名用户")) {
            showNameInputDialog();
        }
    }

    private void showNameInputDialog() {
        // 创建一个简单的对话框，让用户输入昵称
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.enter_nickname);

        // 设置输入框
        final EditText input = new EditText(this);
        builder.setView(input);

        // 设置按钮
        builder.setPositiveButton(R.string.confirm, (dialog, which) -> {
            String nickname = input.getText().toString().trim();
            if (!TextUtils.isEmpty(nickname)) {
                authorName = nickname;
                saveNickname(nickname);
            }
        });

        builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void saveNickname(String nickname) {
        SharedPreferences prefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("username", nickname);
        editor.apply();
    }

    private void setupTags() {
        // 添加预设标签
        String[] tags = {"音乐", "艺术", "心情", "生活", "旅行", "回忆", "感悟", "自然", "城市", "光影"};

        for (String tag : tags) {
            Chip chip = new Chip(this);
            chip.setText(tag);
            chip.setCheckable(true);
            tagChipGroup.addView(chip);
        }
    }

    private void setupListeners() {
        // 选择图片
        selectImageTextView.setOnClickListener(v ->
                imagePickerLauncher.launch("image/*"));

        // 选择音乐
        selectMusicTextView.setOnClickListener(v ->
                musicPickerLauncher.launch("audio/*"));

        // 分享按钮
        shareButton.setOnClickListener(v -> shareContent());
    }

    private void checkIntentData() {
        Intent intent = getIntent();

        // 检查是否有预设的图片URI
        if (intent.hasExtra("image_uri")) {
            String uriString = intent.getStringExtra("image_uri");
            if (uriString != null) {
                selectedImageUri = Uri.parse(uriString);
                loadImage();
            }
        }

        // 检查是否有预设的音乐URI
        if (intent.hasExtra("music_uri")) {
            String uriString = intent.getStringExtra("music_uri");
            if (uriString != null) {
                selectedMusicUri = Uri.parse(uriString);
                updateMusicSelection();
            }
        }

        // 检查是否有预设标题
        if (intent.hasExtra("title")) {
            String title = intent.getStringExtra("title");
            if (title != null) {
                titleEditText.setText(title);
            }
        }

        // 检查是否有预设内容
        if (intent.hasExtra("content")) {
            String content = intent.getStringExtra("content");
            if (content != null) {
                contentEditText.setText(content);
            }
        }
    }

    private void loadImage() {
        if (selectedImageUri != null) {
            Glide.with(this)
                    .load(selectedImageUri)
                    .centerCrop()
                    .into(selectedImageView);

            selectedImageView.setVisibility(View.VISIBLE);
            selectImageTextView.setText(R.string.change_image);
        }
    }

    private void updateMusicSelection() {
        if (selectedMusicUri != null) {
            // 获取文件名
            String fileName = getFileName(selectedMusicUri);
            selectMusicTextView.setText(getString(R.string.selected_music, fileName));
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

    private void shareContent() {
        // 获取输入数据
        String title = titleEditText.getText().toString().trim();
        String content = contentEditText.getText().toString().trim();

        // 验证输入
        if (TextUtils.isEmpty(title)) {
            titleEditText.setError(getString(R.string.title_required));
            return;
        }

        if (TextUtils.isEmpty(content)) {
            contentEditText.setError(getString(R.string.content_required));
            return;
        }

        if (selectedImageUri == null) {
            Toast.makeText(this, R.string.image_required, Toast.LENGTH_SHORT).show();
            return;
        }

        // 显示进度条
        progressBar.setVisibility(View.VISIBLE);
        shareButton.setEnabled(false);

        // 收集已选标签
        List<String> selectedTags = new ArrayList<>();
        for (int i = 0; i < tagChipGroup.getChildCount(); i++) {
            Chip chip = (Chip) tagChipGroup.getChildAt(i);
            if (chip.isChecked()) {
                selectedTags.add(chip.getText().toString());
            }
        }

        // 创建分享项
        saveShareItem(title, content, selectedTags);
    }

    private void saveShareItem(String title, String content, List<String> tags) {
        try {
            // 保存图片到本地
            Bitmap bitmap = ((BitmapDrawable) selectedImageView.getDrawable()).getBitmap();
            String imagePath = saveImageToInternalStorage(bitmap);

            // 创建唯一ID
            String shareId = UUID.randomUUID().toString();

            // 创建分享对象
            SharedPreferences prefs = getSharedPreferences("ShareItems", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();

            // 构造JSON字符串
            StringBuilder json = new StringBuilder();
            json.append("{");
            json.append("\"id\":\"").append(shareId).append("\",");
            json.append("\"title\":\"").append(escapeJson(title)).append("\",");
            json.append("\"author\":\"").append(escapeJson(authorName)).append("\",");
            json.append("\"authorId\":\"").append(deviceId).append("\",");
            json.append("\"content\":\"").append(escapeJson(content)).append("\",");
            json.append("\"imagePath\":\"").append(imagePath).append("\",");
            json.append("\"timestamp\":").append(System.currentTimeMillis()).append(",");
            json.append("\"likes\":0,");

            // 添加音乐路径 (如果有)
            if (selectedMusicUri != null) {
                json.append("\"musicUri\":\"").append(selectedMusicUri.toString()).append("\",");
            }

            // 添加标签
            json.append("\"tags\":[");
            for (int i = 0; i < tags.size(); i++) {
                json.append("\"").append(escapeJson(tags.get(i))).append("\"");
                if (i < tags.size() - 1) {
                    json.append(",");
                }
            }
            json.append("]");

            json.append("}");

            // 保存到SharedPreferences
            editor.putString(shareId, json.toString());

            // 更新分享项列表
            String itemListJson = prefs.getString("itemList", "[]");
            List<String> itemList = parseJsonArray(itemListJson);
            itemList.add(0, shareId); // 添加到列表开头

            // 保存更新后的列表
            editor.putString("itemList", listToJsonArray(itemList));
            editor.apply();

            // 提示用户分享成功
            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, R.string.share_success, Toast.LENGTH_SHORT).show();
                finish();
            });

        } catch (Exception e) {
            e.printStackTrace();
            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                shareButton.setEnabled(true);
                Toast.makeText(this, getString(R.string.share_failed) + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        }
    }

    private String saveImageToInternalStorage(Bitmap bitmap) throws IOException {
        // 创建文件名
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "SHARE_" + timeStamp + ".jpg";

        // 获取内部存储目录
        File directory = new File(getFilesDir(), "shared_images");
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

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private List<String> parseJsonArray(String json) {
        List<String> result = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                result.add(array.getString(i));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return result;
    }

    private String listToJsonArray(List<String> list) {
        JSONArray array = new JSONArray();
        for (String item : list) {
            array.put(item);
        }
        return array.toString();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}