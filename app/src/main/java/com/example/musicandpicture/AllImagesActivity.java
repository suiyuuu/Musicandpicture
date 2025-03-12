package com.example.musicandpicture;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class AllImagesActivity extends AppCompatActivity {

    private RecyclerView allImagesRecyclerView;
    private TextView emptyImagesTextView;
    private MediaAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_images);

        // 设置ActionBar的返回按钮（不使用自定义Toolbar）
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.all_images);
        }

        allImagesRecyclerView = findViewById(R.id.allImagesRecyclerView);
        emptyImagesTextView = findViewById(R.id.emptyImagesTextView);

        // 设置网格布局，3列
        allImagesRecyclerView.setLayoutManager(new GridLayoutManager(this, 3));

        // 设置适配器
        adapter = new MediaAdapter(this, MainActivity.globalImageList, true);
        allImagesRecyclerView.setAdapter(adapter);

        // 根据图片列表状态更新UI
        updateUI();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateUI() {
        if (MainActivity.globalImageList.isEmpty()) {
            allImagesRecyclerView.setVisibility(View.GONE);
            emptyImagesTextView.setVisibility(View.VISIBLE);
        } else {
            allImagesRecyclerView.setVisibility(View.VISIBLE);
            emptyImagesTextView.setVisibility(View.GONE);
        }
    }
}