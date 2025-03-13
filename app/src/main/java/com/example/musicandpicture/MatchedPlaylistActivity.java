package com.example.musicandpicture;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.palette.graphics.Palette;
import androidx.viewpager2.widget.ViewPager2;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class MatchedPlaylistActivity extends AppCompatActivity {

    private static final int SLIDESHOW_INTERVAL = 6000; // 6秒切换一次图片

    private ViewPager2 slideshowViewPager;
    private ImageView backgroundBlurView;
    private TextView nowPlayingText;
    private TextView artistText;
    private SeekBar musicSeekBar;
    private TextView currentTimeText;
    private TextView totalTimeText;
    private ImageButton playPauseButton;
    private ImageButton prevButton;
    private ImageButton nextButton;
    private ImageButton closeButton;
    private ProgressBar loadingProgressBar;

    private MediaPlayer mediaPlayer;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable updateSeekBarRunnable;
    private Timer slideshowTimer;

    private List<MediaItem> matchedMusicList;
    private List<Bitmap> colorMatchedImages = new ArrayList<>();
    private int currentMusicIndex = 0;
    private boolean isPlaying = false;
    private boolean isActivityResumed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 设置全屏
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_matched_playlist);

        // 初始化视图
        initViews();

        // 从全局变量获取匹配的音乐列表
        matchedMusicList = new ArrayList<>(MainActivity.matchedMusicList);

        // 获取传递的图片URI
        String imageUriString = getIntent().getStringExtra("image_uri");
        if (imageUriString != null) {
            Uri imageUri = Uri.parse(imageUriString);
            loadImageAndGenerateVariants(imageUri);
        }

        // 设置监听器
        setupListeners();

        // 开始播放第一首歌
        if (!matchedMusicList.isEmpty()) {
            playMusic(0);
        } else {
            Toast.makeText(this, R.string.no_matched_music, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initViews() {
        slideshowViewPager = findViewById(R.id.slideshowViewPager);
        backgroundBlurView = findViewById(R.id.backgroundBlurView);
        nowPlayingText = findViewById(R.id.nowPlayingText);
        artistText = findViewById(R.id.artistText);
        musicSeekBar = findViewById(R.id.musicSeekBar);
        currentTimeText = findViewById(R.id.currentTimeText);
        totalTimeText = findViewById(R.id.totalTimeText);
        playPauseButton = findViewById(R.id.playPauseButton);
        prevButton = findViewById(R.id.prevButton);
        nextButton = findViewById(R.id.nextButton);
        closeButton = findViewById(R.id.closeButton);
        loadingProgressBar = findViewById(R.id.loadingProgressBar);
    }

    private void setupListeners() {
        // 播放/暂停按钮
        playPauseButton.setOnClickListener(v -> {
            if (isPlaying) {
                pauseMusic();
            } else {
                resumeMusic();
            }
        });

        // 上一首按钮
        prevButton.setOnClickListener(v -> {
            playPrevious();
        });

        // 下一首按钮
        nextButton.setOnClickListener(v -> {
            playNext();
        });

        // 关闭按钮
        closeButton.setOnClickListener(v -> {
            finish();
        });

        // 进度条
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

    /**
     * 加载原始图片并生成变体
     */
    private void loadImageAndGenerateVariants(Uri imageUri) {
        try {
            // 显示加载进度条
            loadingProgressBar.setVisibility(View.VISIBLE);

            // 在后台线程中处理图片
            new Thread(() -> {
                try {
                    // 加载原始图片
                    Bitmap originalBitmap = BitmapFactory.decodeStream(
                            getContentResolver().openInputStream(imageUri));

                    if (originalBitmap == null) {
                        throw new Exception("Failed to load image");
                    }

                    // 添加原始图片
                    colorMatchedImages.add(originalBitmap);

                    // 使用Palette API提取图片的主要颜色
                    Palette palette = Palette.from(originalBitmap).generate();

                    // 获取不同色调的颜色
                    int vibrantColor = palette.getVibrantColor(Color.BLACK);
                    int darkVibrantColor = palette.getDarkVibrantColor(Color.BLACK);
                    int lightVibrantColor = palette.getLightVibrantColor(Color.BLACK);
                    int mutedColor = palette.getMutedColor(Color.BLACK);
                    int darkMutedColor = palette.getDarkMutedColor(Color.BLACK);

                    // 生成不同色调的图片变体
                    colorMatchedImages.add(createColorFilteredImage(originalBitmap, vibrantColor));
                    colorMatchedImages.add(createColorFilteredImage(originalBitmap, darkVibrantColor));
                    colorMatchedImages.add(createColorFilteredImage(originalBitmap, lightVibrantColor));
                    colorMatchedImages.add(createColorFilteredImage(originalBitmap, mutedColor));
                    colorMatchedImages.add(createColorFilteredImage(originalBitmap, darkMutedColor));

                    // 在UI线程中更新界面
                    runOnUiThread(() -> {
                        // 设置ViewPager2的适配器
                        ImageSlideshowAdapter adapter = new ImageSlideshowAdapter(this, colorMatchedImages);
                        slideshowViewPager.setAdapter(adapter);

                        // 设置背景虚化图片
                        backgroundBlurView.setImageBitmap(createBlurredImage(originalBitmap));

                        // 隐藏加载进度条
                        loadingProgressBar.setVisibility(View.GONE);

                        // 开始幻灯片播放
                        startSlideshow();
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> {
                        Toast.makeText(MatchedPlaylistActivity.this,
                                R.string.error_processing_image, Toast.LENGTH_SHORT).show();
                        finish();
                    });
                }
            }).start();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, R.string.error_loading_image, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /**
     * 创建使用特定颜色滤镜的图片
     */
    private Bitmap createColorFilteredImage(Bitmap original, int color) {
        // 创建新的空白Bitmap
        Bitmap filteredBitmap = Bitmap.createBitmap(
                original.getWidth(), original.getHeight(), original.getConfig());

        // 创建画布和颜色过滤器
        android.graphics.Canvas canvas = new android.graphics.Canvas(filteredBitmap);
        android.graphics.Paint paint = new android.graphics.Paint();

        // 提取颜色的RGB值，创建过滤矩阵
        float r = Color.red(color) / 255f;
        float g = Color.green(color) / 255f;
        float b = Color.blue(color) / 255f;

        // 创建颜色矩阵，调整色调
        float[] colorMatrix = new float[] {
                r, 0, 0, 0, 0,
                0, g, 0, 0, 0,
                0, 0, b, 0, 0,
                0, 0, 0, 1, 0
        };

        // 应用颜色矩阵
        android.graphics.ColorMatrix cm = new android.graphics.ColorMatrix();
        cm.set(colorMatrix);

        // 使用颜色过滤器
        paint.setColorFilter(new android.graphics.ColorMatrixColorFilter(cm));

        // 在画布上绘制原始Bitmap
        canvas.drawBitmap(original, 0, 0, paint);

        return filteredBitmap;
    }

    /**
     * 创建模糊效果的图片
     */
    private Bitmap createBlurredImage(Bitmap original) {
        // 创建较小的图片以提高模糊处理性能
        Bitmap input = Bitmap.createScaledBitmap(original,
                original.getWidth() / 4, original.getHeight() / 4, true);

        // 创建空白Bitmap
        Bitmap output = Bitmap.createBitmap(
                input.getWidth(), input.getHeight(), input.getConfig());

        // 使用RenderScript进行模糊处理
        android.renderscript.RenderScript rs = android.renderscript.RenderScript.create(this);
        android.renderscript.ScriptIntrinsicBlur script =
                android.renderscript.ScriptIntrinsicBlur.create(
                        rs, android.renderscript.Element.U8_4(rs));

        android.renderscript.Allocation inAlloc =
                android.renderscript.Allocation.createFromBitmap(rs, input);
        android.renderscript.Allocation outAlloc =
                android.renderscript.Allocation.createFromBitmap(rs, output);

        // 设置模糊半径（1-25之间）
        script.setRadius(25f);
        script.setInput(inAlloc);
        script.forEach(outAlloc);

        // 将结果复制到输出Bitmap
        outAlloc.copyTo(output);

        // 释放RenderScript资源
        rs.destroy();

        // 应用深色覆盖，使背景更暗
        android.graphics.Canvas canvas = new android.graphics.Canvas(output);
        android.graphics.Paint paint = new android.graphics.Paint();
        paint.setColor(Color.BLACK);
        paint.setAlpha(180); // 设置透明度
        canvas.drawRect(0, 0, output.getWidth(), output.getHeight(), paint);

        return output;
    }

    /**
     * 开始幻灯片播放
     */
    private void startSlideshow() {
        // 如果已有计时器，先停止
        stopSlideshow();

        // 创建新的计时器
        slideshowTimer = new Timer();
        slideshowTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (isActivityResumed && slideshowViewPager != null) {
                    runOnUiThread(() -> {
                        // 切换到下一张图片
                        int nextItem = (slideshowViewPager.getCurrentItem() + 1) % colorMatchedImages.size();
                        slideshowViewPager.setCurrentItem(nextItem, true);
                    });
                }
            }
        }, SLIDESHOW_INTERVAL, SLIDESHOW_INTERVAL);
    }

    /**
     * 停止幻灯片播放
     */
    private void stopSlideshow() {
        if (slideshowTimer != null) {
            slideshowTimer.cancel();
            slideshowTimer = null;
        }
    }

    /**
     * 播放指定索引的音乐
     */
    private void playMusic(int index) {
        // 验证索引
        if (index < 0 || index >= matchedMusicList.size()) {
            return;
        }

        // 保存当前索引
        currentMusicIndex = index;
        MediaItem musicItem = matchedMusicList.get(index);

        try {
            // 释放旧的MediaPlayer
            releaseMediaPlayer();

            // 创建新的MediaPlayer
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(this, musicItem.getUri());
            mediaPlayer.prepare();

            // 设置播放完成监听器
            mediaPlayer.setOnCompletionListener(mp -> {
                playNext();
            });

            // 开始播放
            mediaPlayer.start();
            isPlaying = true;

            // 更新UI
            updateMusicUI(musicItem);
            updatePlayPauseButton();

            // 开始更新进度条
            startProgressUpdate();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, R.string.error_playing_music, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 播放上一首歌
     */
    private void playPrevious() {
        int prevIndex = currentMusicIndex - 1;
        if (prevIndex < 0) {
            prevIndex = matchedMusicList.size() - 1;
        }
        playMusic(prevIndex);
    }

    /**
     * 播放下一首歌
     */
    private void playNext() {
        int nextIndex = (currentMusicIndex + 1) % matchedMusicList.size();
        playMusic(nextIndex);
    }

    /**
     * 暂停音乐
     */
    private void pauseMusic() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            isPlaying = false;
            updatePlayPauseButton();

            // 停止更新进度条
            handler.removeCallbacks(updateSeekBarRunnable);
        }
    }

    /**
     * 恢复播放音乐
     */
    private void resumeMusic() {
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            isPlaying = true;
            updatePlayPauseButton();

            // 继续更新进度条
            startProgressUpdate();
        }
    }

    /**
     * 更新音乐UI
     */
    private void updateMusicUI(MediaItem musicItem) {
        // 设置歌曲名和艺术家名
        nowPlayingText.setText(musicItem.getSongName() != null ?
                musicItem.getSongName() : musicItem.getFileName());
        artistText.setText(musicItem.getArtistName() != null ?
                musicItem.getArtistName() : getString(R.string.unknown_artist));

        // 设置进度条最大值
        int duration = mediaPlayer.getDuration();
        musicSeekBar.setMax(duration);
        totalTimeText.setText(formatTime(duration));
        currentTimeText.setText(formatTime(0));

        // 应用淡入动画
        AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
        fadeIn.setDuration(500);
        nowPlayingText.startAnimation(fadeIn);
        artistText.startAnimation(fadeIn);
    }

    /**
     * 更新播放/暂停按钮状态
     */
    private void updatePlayPauseButton() {
        if (isPlaying) {
            playPauseButton.setImageResource(R.drawable.ic_pause);
        } else {
            playPauseButton.setImageResource(R.drawable.ic_play);
        }
    }

    /**
     * 开始更新进度条
     */
    private void startProgressUpdate() {
        // 创建Runnable用于更新进度条
        updateSeekBarRunnable = new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && isPlaying) {
                    int currentPosition = mediaPlayer.getCurrentPosition();
                    musicSeekBar.setProgress(currentPosition);
                    updateCurrentTimeText();

                    // 每200毫秒更新一次
                    handler.postDelayed(this, 200);
                }
            }
        };

        // 开始更新
        handler.post(updateSeekBarRunnable);
    }

    /**
     * 更新当前时间文本
     */
    private void updateCurrentTimeText() {
        if (mediaPlayer != null) {
            currentTimeText.setText(formatTime(mediaPlayer.getCurrentPosition()));
        }
    }

    /**
     * 格式化时间为分:秒格式
     */
    private String formatTime(int timeMs) {
        int minutes = timeMs / 1000 / 60;
        int seconds = timeMs / 1000 % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    /**
     * 释放MediaPlayer资源
     */
    private void releaseMediaPlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
            isPlaying = false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        isActivityResumed = true;

        // 如果幻灯片已设置，重新开始播放
        if (slideshowViewPager.getAdapter() != null) {
            startSlideshow();
        }

        // 如果音乐已暂停，恢复播放
        if (mediaPlayer != null && !mediaPlayer.isPlaying() && isPlaying) {
            mediaPlayer.start();
            startProgressUpdate();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        isActivityResumed = false;

        // 暂停幻灯片播放
        stopSlideshow();

        // 暂停音乐播放
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            handler.removeCallbacks(updateSeekBarRunnable);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // 释放资源
        stopSlideshow();
        releaseMediaPlayer();
        handler.removeCallbacks(updateSeekBarRunnable);

        // 清除图片列表，避免内存泄漏
        for (Bitmap bitmap : colorMatchedImages) {
            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
            }
        }
        colorMatchedImages.clear();
    }
}