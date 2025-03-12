package com.example.musicandpicture;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.example.musicandpicture.api.ApiService;
import com.example.musicandpicture.model.SongResponse;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MusicPlayerActivity extends AppCompatActivity
        implements KeywordsFragment.OnGenerateStoryListener {

    private static final String TAG = "MusicPlayerActivity";

    private Toolbar toolbar;
    private ImageView albumArtImageView;
    private TextView songTitleTextView;
    private TextView artistTextView;
    private SeekBar musicSeekBar;
    private TextView currentTimeTextView;
    private TextView totalTimeTextView;
    private ImageButton playPauseButton;
    private ImageButton previousButton;
    private ImageButton nextButton;
    private TabLayout contentTabLayout;
    private ViewPager2 contentViewPager;
    private ProgressBar apiLoadingProgressBar;

    private MediaPlayer mediaPlayer;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable updateSeekBarRunnable;
    private boolean isPlaying = false;
    private Uri musicUri;
    private String songName;
    private String artistName;
    private String fileName;
    private String lyrics = "正在加载歌词...";
    private ArrayList<String> keywords = new ArrayList<>();
    private String story = "正在生成故事...";

    private LyricsFragment lyricsFragment;
    private KeywordsFragment keywordsFragment;
    private StoryFragment storyFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_player);

        // 获取传递过来的数据
        musicUri = getIntent().getParcelableExtra("music_uri");
        songName = getIntent().getStringExtra("song_name");
        artistName = getIntent().getStringExtra("artist_name");
        fileName = getIntent().getStringExtra("file_name"); // 获取文件名

        // 初始化视图
        initViews();
        setupToolbar();
        setupMediaPlayer();
        setupFragments();
        updateUI();

        // 调用API获取歌词、关键词和故事
        callApiWithFileInfo();
    }

    private void initViews() {
        toolbar = findViewById(R.id.musicPlayerToolbar);
        albumArtImageView = findViewById(R.id.albumArtImageView);
        songTitleTextView = findViewById(R.id.songTitleTextView);
        artistTextView = findViewById(R.id.artistTextView);
        musicSeekBar = findViewById(R.id.musicSeekBar);
        currentTimeTextView = findViewById(R.id.currentTimeTextView);
        totalTimeTextView = findViewById(R.id.totalTimeTextView);
        playPauseButton = findViewById(R.id.playPauseButton);
        previousButton = findViewById(R.id.previousButton);
        nextButton = findViewById(R.id.nextButton);
        contentTabLayout = findViewById(R.id.contentTabLayout);
        contentViewPager = findViewById(R.id.contentViewPager);

        // 尝试查找API加载进度条
        try {
            apiLoadingProgressBar = findViewById(R.id.apiLoadingProgressBar);
        } catch (Exception e) {
            Log.w(TAG, "apiLoadingProgressBar not found in layout");
        }

        // 设置播放控制按钮的点击事件
        playPauseButton.setOnClickListener(v -> togglePlayPause());
        previousButton.setOnClickListener(v -> playPrevious());
        nextButton.setOnClickListener(v -> playNext());

        // 设置进度条变化监听器
        musicSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && mediaPlayer != null) {
                    mediaPlayer.seekTo(progress);
                    updateCurrentTimeText();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // 不需要操作
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // 不需要操作
            }
        });
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("");
        }
    }

    private void setupFragments() {
        // 创建片段实例
        lyricsFragment = new LyricsFragment();
        keywordsFragment = new KeywordsFragment();
        storyFragment = new StoryFragment();

        // 设置参数 - 初始化时不需要传递内容，将在API响应后更新
        Bundle emptyBundle = new Bundle();
        lyricsFragment.setArguments(emptyBundle);
        keywordsFragment.setArguments(emptyBundle);
        storyFragment.setArguments(emptyBundle);

        // 设置ViewPager适配器
        ContentPagerAdapter pagerAdapter = new ContentPagerAdapter(this);
        contentViewPager.setAdapter(pagerAdapter);

        // 连接TabLayout和ViewPager
        new TabLayoutMediator(contentTabLayout, contentViewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText(R.string.tab_lyrics);
                    break;
                case 1:
                    tab.setText(R.string.tab_keywords);
                    break;
                case 2:
                    tab.setText(R.string.tab_story);
                    break;
            }
        }).attach();
    }

    private void callApiWithFileInfo() {
        if (fileName == null || fileName.isEmpty()) {
            Toast.makeText(this, "文件名无效，无法获取歌曲信息", Toast.LENGTH_SHORT).show();
            return;
        }

        // 显示加载状态
        if (lyricsFragment != null) lyricsFragment.showLoading();
        if (keywordsFragment != null) keywordsFragment.showLoading();
        if (storyFragment != null) storyFragment.showLoading();
        if (apiLoadingProgressBar != null) apiLoadingProgressBar.setVisibility(View.VISIBLE);

        // 调用API
        ApiService apiService = new ApiService();
        apiService.processSong(fileName, new Callback<SongResponse>() {
            @Override
            public void onResponse(Call<SongResponse> call, Response<SongResponse> response) {
                if (apiLoadingProgressBar != null) apiLoadingProgressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    SongResponse songResponse = response.body();

                    // 更新数据
                    lyrics = songResponse.getLyrics() != null ? songResponse.getLyrics() : "未找到歌词";

                    if (songResponse.getKeywords() != null) {
                        keywords = new ArrayList<>(songResponse.getKeywords());
                    } else {
                        keywords = new ArrayList<>();
                    }

                    story = songResponse.getStory() != null ? songResponse.getStory() : "未能生成故事";

                    // 更新UI
                    updateFragmentsWithApiData();
                } else {
                    // 处理错误
                    handleApiError("服务器返回错误: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<SongResponse> call, Throwable t) {
                if (apiLoadingProgressBar != null) apiLoadingProgressBar.setVisibility(View.GONE);
                handleApiError("网络请求失败: " + t.getMessage());
            }
        });
    }

    private void handleApiError(String errorMessage) {
        // 显示错误状态
        if (lyricsFragment != null) lyricsFragment.showError();
        if (keywordsFragment != null) keywordsFragment.showError();
        if (storyFragment != null) storyFragment.showError();

        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
    }

    private void updateFragmentsWithApiData() {
        try {
            // 更新歌词Fragment
            if (lyricsFragment != null) {
                lyricsFragment.updateLyrics(lyrics != null ? lyrics : "未找到歌词");
            }

            // 更新关键词Fragment - 添加空值检查
            if (keywordsFragment != null) {
                if (keywords != null && !keywords.isEmpty()) {
                    String[] keywordArray = keywords.toArray(new String[0]);
                    keywordsFragment.updateKeywords(keywordArray);
                } else {
                    keywordsFragment.updateKeywords(new String[]{"无关键词"});
                }
            }

            // 更新故事Fragment - 添加空值检查
            if (storyFragment != null && musicUri != null) {
                String keywordsStr = keywords != null && !keywords.isEmpty() ?
                        String.join(", ", keywords) : "";
                storyFragment.updateStory(
                        musicUri.toString(),
                        story != null ? story : "无法生成故事",
                        keywordsStr
                );
            }
        } catch (Exception e) {
            Log.e(TAG, "更新Fragment数据出错: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onGenerateStory(List<String> selectedKeywords) {
        Toast.makeText(this, "正在处理您选择的关键词...", Toast.LENGTH_SHORT).show();

        if (storyFragment != null && musicUri != null) {
            String keywordsStr = selectedKeywords != null && !selectedKeywords.isEmpty() ?
                    String.join(", ", selectedKeywords) : "";

            // 如果已经有故事，使用现有故事
            String storyToUse = story;

            // 如果没有故事或故事生成失败，提供一个编辑模板
            if (storyToUse == null || storyToUse.isEmpty() ||
                    storyToUse.equals("无法生成故事") || storyToUse.equals("正在生成故事...")) {
                storyToUse = "基于关键词: " + keywordsStr + "\n\n" +
                        "请在此编辑您的故事...";
            }

            // 更新故事Fragment显示内容
            storyFragment.updateStory(musicUri.toString(), storyToUse, keywordsStr);

            // 自动切换到故事选项卡
            contentViewPager.setCurrentItem(2, true);
        }
    }

    private void setupMediaPlayer() {
        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(this, musicUri);
            mediaPlayer.prepare();

            // 设置进度条最大值
            int duration = mediaPlayer.getDuration();
            musicSeekBar.setMax(duration);
            totalTimeTextView.setText(formatTime(duration));

            // 设置播放完成监听器
            mediaPlayer.setOnCompletionListener(mp -> {
                playPauseButton.setImageResource(R.drawable.ic_play);
                isPlaying = false;
                handler.removeCallbacks(updateSeekBarRunnable);
            });

            // 创建更新进度条的Runnable
            updateSeekBarRunnable = new Runnable() {
                @Override
                public void run() {
                    if (mediaPlayer != null && isPlaying) {
                        int currentPosition = mediaPlayer.getCurrentPosition();
                        musicSeekBar.setProgress(currentPosition);
                        updateCurrentTimeText();
                        handler.postDelayed(this, 1000);
                    }
                }
            };

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error loading media", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateUI() {
        // 设置歌曲标题和艺术家名称
        songTitleTextView.setText(songName);
        artistTextView.setText(artistName);

        // 设置默认专辑封面
        Glide.with(this)
                .load(R.drawable.default_album_art)
                .centerCrop()
                .into(albumArtImageView);
    }

    private void togglePlayPause() {
        if (mediaPlayer != null) {
            if (isPlaying) {
                mediaPlayer.pause();
                playPauseButton.setImageResource(R.drawable.ic_play);
                handler.removeCallbacks(updateSeekBarRunnable);
            } else {
                mediaPlayer.start();
                playPauseButton.setImageResource(R.drawable.ic_pause);
                handler.post(updateSeekBarRunnable);
            }
            isPlaying = !isPlaying;
        }
    }

    private void playPrevious() {
        // 目前仅重置当前歌曲
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(0);
            musicSeekBar.setProgress(0);
            updateCurrentTimeText();

            if (!isPlaying) {
                togglePlayPause();
            }
        }
    }

    private void playNext() {
        // 目前仅重置当前歌曲
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(0);
            musicSeekBar.setProgress(0);
            updateCurrentTimeText();

            if (!isPlaying) {
                togglePlayPause();
            }
        }
    }

    private void updateCurrentTimeText() {
        if (mediaPlayer != null) {
            int currentPosition = mediaPlayer.getCurrentPosition();
            currentTimeTextView.setText(formatTime(currentPosition));
        }
    }

    private String formatTime(int timeMs) {
        int minutes = timeMs / 1000 / 60;
        int seconds = timeMs / 1000 % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
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
            if (isPlaying) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
        handler.removeCallbacks(updateSeekBarRunnable);
    }

    // ViewPager2适配器
    private class ContentPagerAdapter extends FragmentStateAdapter {

        public ContentPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0:
                    return lyricsFragment;
                case 1:
                    return keywordsFragment;
                case 2:
                    return storyFragment;
                default:
                    return lyricsFragment;
            }
        }

        @Override
        public int getItemCount() {
            return 3;
        }
    }
}