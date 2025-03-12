package com.example.musicandpicture;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager2.widget.ViewPager2;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MidiPlayerActivity extends AppCompatActivity {

    private static final String TAG = "MidiPlayerActivity";
    private static final int SLIDESHOW_INTERVAL_SLOW = 7000;    // 7秒
    private static final int SLIDESHOW_INTERVAL_MEDIUM = 5000;  // 5秒
    private static final int SLIDESHOW_INTERVAL_FAST = 3000;    // 3秒

    private ViewPager2 slideshowPager;
    private TextView noImagesText;
    private TextView midiFileName;
    private Spinner instrumentSpinner;
    private SeekBar midiSeekBar;
    private TextView currentTimeText;
    private TextView totalTimeText;
    private ImageButton playPauseButton;
    private ImageButton prevButton;
    private ImageButton nextButton;

    private Uri midiUri;
    private String midiFileNameText;
    private MediaPlayer mediaPlayer;
    private boolean isPlaying = false;
    private int currentInstrument = 0; // 默认音色
    private int slideshowSpeed = SLIDESHOW_INTERVAL_MEDIUM; // 默认速度

    private Handler handler = new Handler();
    private Runnable slideshowRunnable;
    private SlideshowAdapter slideshowAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_midi_player);

        // 获取传递过来的MIDI文件URI
        if (getIntent().hasExtra("midi_uri")) {
            midiUri = getIntent().getParcelableExtra("midi_uri");
            midiFileNameText = getIntent().getStringExtra("midi_filename");
        }

        initViews();
        setupToolbar();
        setupInstrumentSpinner();
        setupSlideshow();
        setupMediaPlayer();
        setupControls();
    }

    private void initViews() {
        slideshowPager = findViewById(R.id.slideshowPager);
        noImagesText = findViewById(R.id.noImagesText);
        midiFileName = findViewById(R.id.midiFileName);
        instrumentSpinner = findViewById(R.id.instrumentSpinner);
        midiSeekBar = findViewById(R.id.midiSeekBar);
        currentTimeText = findViewById(R.id.currentTimeText);
        totalTimeText = findViewById(R.id.totalTimeText);
        playPauseButton = findViewById(R.id.playPauseButton);
        prevButton = findViewById(R.id.prevButton);
        nextButton = findViewById(R.id.nextButton);

        // 设置MIDI文件名
        if (midiFileNameText != null) {
            midiFileName.setText(midiFileNameText);
        } else {
            midiFileName.setText(R.string.no_midi_selected);
        }
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.midi_player_title);
        }
    }

    private void setupInstrumentSpinner() {
        // 创建通用MIDI音色列表
        String[] instruments = new String[] {
                "大钢琴", "明亮的钢琴", "电钢琴", "酒吧钢琴", "柔和的电钢琴", "铁琴", "拨弦古钢琴", "大键琴",
                "钢片琴", "钟琴", "音乐盒", "颤音琴", "马林巴", "木琴", "管钟", "大扬琴",
                "抽音风琴", "打击风琴", "摇滚风琴", "教堂风琴", "簧风琴", "手风琴", "口琴", "探戈手风琴",
                "尼龙弦吉他", "钢弦吉他", "爵士电吉他", "清音电吉他", "闷音电吉他", "过载电吉他", "失真电吉他", "吉他和音",
                "原声贝司", "指弹电贝司", "拨片电贝司", "无品贝司", "击弦贝司1", "击弦贝司2", "合成贝司1", "合成贝司2",
                "小提琴", "中提琴", "大提琴", "低音提琴", "弦乐震音", "弦乐拨奏", "竖琴", "定音鼓",
                "弦乐合奏1", "弦乐合奏2", "合成弦乐1", "合成弦乐2", "唱诗班Aahs", "人声Oohs", "合成人声", "管弦打击乐",
                "小号", "长号", "大号", "闷音小号", "法国号", "铜管乐", "合成铜管1", "合成铜管2",
                "高音萨克斯", "中音萨克斯", "次中音萨克斯", "低音萨克斯", "双簧管", "英国管", "巴松管", "单簧管",
                "短笛", "长笛", "竖笛", "排笛", "瓶笛", "日本尺八", "哨笛", "苏格兰风笛",
                "方波合成", "锯齿波合成", "电子琴", "带合唱效果电子琴", "复音合成", "弦乐合成", "金属合成", "合成竖琴",
                "音符合成", "新世纪合成", "温暖合成", "复音合成", "民族合成", "回声叮当", "天籁音色", "金属音色",
                "西塔琴", "班卓琴", "三味线", "筝", "古筝", "卡林巴", "风笛", "小提琴",
                "山奈", "钟琴", "阿格奥戈钟", "钢鼓", "木鱼", "太鼓", "中提琴独奏", "定音鼓",
                "吉他弹奏", "贝斯弹奏", "拨弦音效", "呼吸声", "海浪声", "鸟鸣声", "电话铃声", "直升机声",
                "掌声", "枪声"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, instruments);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        instrumentSpinner.setAdapter(adapter);

        instrumentSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentInstrument = position;
                // 这里我们会在实际的MIDI播放实现中使用这个音色
                Toast.makeText(MidiPlayerActivity.this, "已选择音色: " + instruments[position], Toast.LENGTH_SHORT).show();

                // 如果更改音色，需要重新加载MIDI文件并应用新音色
                if (isPlaying) {
                    stopPlayback();
                    startPlayback();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // 不做任何事
            }
        });
    }

    private void setupSlideshow() {
        List<MediaItem> imageItems = new ArrayList<>(MainActivity.globalImageList);

        if (imageItems.isEmpty()) {
            noImagesText.setVisibility(View.VISIBLE);
            slideshowPager.setVisibility(View.GONE);
        } else {
            noImagesText.setVisibility(View.GONE);
            slideshowPager.setVisibility(View.VISIBLE);

            slideshowAdapter = new SlideshowAdapter(this, imageItems);
            slideshowPager.setAdapter(slideshowAdapter);

            // 设置自动滚动
            slideshowRunnable = new Runnable() {
                @Override
                public void run() {
                    if (slideshowPager.getCurrentItem() == slideshowAdapter.getItemCount() - 1) {
                        slideshowPager.setCurrentItem(0); // 循环到第一张
                    } else {
                        slideshowPager.setCurrentItem(slideshowPager.getCurrentItem() + 1);
                    }
                    handler.postDelayed(this, slideshowSpeed);
                }
            };
        }
    }

    private void setupMediaPlayer() {
        try {
            if (midiUri != null) {
                mediaPlayer = MediaPlayer.create(this, midiUri);

                if (mediaPlayer != null) {
                    mediaPlayer.setOnCompletionListener(mp -> {
                        playPauseButton.setImageResource(R.drawable.ic_play);
                        isPlaying = false;
                        handler.removeCallbacks(slideshowRunnable);
                    });

                    // 设置进度条
                    midiSeekBar.setMax(mediaPlayer.getDuration());
                    totalTimeText.setText(formatTime(mediaPlayer.getDuration()));

                    // 进度条变化监听
                    midiSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                        @Override
                        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                            if (fromUser) {
                                mediaPlayer.seekTo(progress);
                                currentTimeText.setText(formatTime(progress));
                            }
                        }

                        @Override
                        public void onStartTrackingTouch(SeekBar seekBar) {
                            // 不做任何事
                        }

                        @Override
                        public void onStopTrackingTouch(SeekBar seekBar) {
                            // 不做任何事
                        }
                    });

                    // 更新进度条的线程
                    MidiPlayerActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (mediaPlayer != null && isPlaying) {
                                int currentPosition = mediaPlayer.getCurrentPosition();
                                midiSeekBar.setProgress(currentPosition);
                                currentTimeText.setText(formatTime(currentPosition));
                            }
                            handler.postDelayed(this, 1000);
                        }
                    });
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up media player", e);
            Toast.makeText(this, "无法播放MIDI文件", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupControls() {
        // 播放/暂停按钮
        playPauseButton.setOnClickListener(v -> {
            if (isPlaying) {
                pausePlayback();
            } else {
                startPlayback();
            }
        });

        // 上一张/下一张图片
        prevButton.setOnClickListener(v -> {
            if (slideshowPager.getCurrentItem() > 0) {
                slideshowPager.setCurrentItem(slideshowPager.getCurrentItem() - 1);
            }
        });

        nextButton.setOnClickListener(v -> {
            if (slideshowPager.getCurrentItem() < slideshowAdapter.getItemCount() - 1) {
                slideshowPager.setCurrentItem(slideshowPager.getCurrentItem() + 1);
            }
        });
    }

    private void startPlayback() {
        if (mediaPlayer != null && !isPlaying) {
            mediaPlayer.start();
            isPlaying = true;
            playPauseButton.setImageResource(R.drawable.ic_pause);

            // 开始幻灯片播放
            if (slideshowAdapter != null && slideshowAdapter.getItemCount() > 0) {
                handler.postDelayed(slideshowRunnable, slideshowSpeed);
            }
        }
    }

    private void pausePlayback() {
        if (mediaPlayer != null && isPlaying) {
            mediaPlayer.pause();
            isPlaying = false;
            playPauseButton.setImageResource(R.drawable.ic_play);

            // 暂停幻灯片播放
            handler.removeCallbacks(slideshowRunnable);
        }
    }

    private void stopPlayback() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.reset();
            isPlaying = false;
            playPauseButton.setImageResource(R.drawable.ic_play);

            // 停止幻灯片播放
            handler.removeCallbacks(slideshowRunnable);
        }
    }

    private String formatTime(int timeMs) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(timeMs);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(timeMs) -
                TimeUnit.MINUTES.toSeconds(minutes);
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
    protected void onPause() {
        super.onPause();
        pausePlayback();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        handler.removeCallbacks(slideshowRunnable);
    }
}