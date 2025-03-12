package com.example.musicandpicture;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class AllMusicActivity extends AppCompatActivity {

    private RecyclerView allMusicRecyclerView;
    private TextView emptyMusicTextView;
    private MusicAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_music);

        // 设置ActionBar的返回按钮（不使用自定义Toolbar）
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.all_music);
        }

        allMusicRecyclerView = findViewById(R.id.allMusicRecyclerView);
        emptyMusicTextView = findViewById(R.id.emptyMusicTextView);

        // 设置线性布局
        allMusicRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // 设置适配器
        adapter = new MusicAdapter(this, MainActivity.globalMusicList);
        allMusicRecyclerView.setAdapter(adapter);

        // 根据音乐列表状态更新UI
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
        if (MainActivity.globalMusicList.isEmpty()) {
            allMusicRecyclerView.setVisibility(View.GONE);
            emptyMusicTextView.setVisibility(View.VISIBLE);
        } else {
            allMusicRecyclerView.setVisibility(View.VISIBLE);
            emptyMusicTextView.setVisibility(View.GONE);
        }
    }
}