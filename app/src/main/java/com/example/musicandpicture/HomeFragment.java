package com.example.musicandpicture;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.palette.graphics.Palette;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";
    private static final int MAX_VISIBLE_IMAGES = 6;
    private static final int MAX_VISIBLE_MUSIC = 5;

    private RecyclerView imageRecyclerView;
    private RecyclerView musicRecyclerView;
    private FloatingActionButton addImageButton;
    private FloatingActionButton addMusicButton;
    private Button viewAllImagesButton;
    private Button viewAllMusicButton;
    private Button importMidiButton;

    // 新增的匹配区域相关控件
    private CardView selectImageCard;
    private ImageView selectedImageView;
    private TextView selectImageText;
    private Button matchButton;
    private ProgressBar matchProgressBar;
    private TextView matchPlaylistTitle;
    private Button playMatchedPlaylistButton;

    private List<MediaItem> imageList = new ArrayList<>();
    private List<MediaItem> musicList = new ArrayList<>();
    private List<MediaItem> matchedMusicList = new ArrayList<>();
    private MediaAdapter imageAdapter;
    private MusicAdapter musicAdapter;
    private MusicAdapter matchedMusicAdapter;

    private Uri selectedImageUri;
    private Bitmap selectedImageBitmap;
    private boolean isMatchMode = false;

    // 使用 ActivityResultLauncher 处理图片选择结果
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    // 使用 ActivityResultLauncher 处理匹配图片选择结果
    private ActivityResultLauncher<Intent> matchImagePickerLauncher;

    // 使用 ActivityResultLauncher 处理音乐选择结果
    private ActivityResultLauncher<Intent> musicPickerLauncher;

    // 使用 ActivityResultLauncher 处理MIDI选择结果
    private ActivityResultLauncher<Intent> midiPickerLauncher;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 初始化结果接收器
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                        Uri selectedImageUri = result.getData().getData();
                        if (selectedImageUri != null) {
                            MediaItem imageItem = new MediaItem();
                            imageItem.setUri(selectedImageUri);
                            imageItem.setFileName(((MainActivity) getActivity()).getFileNameFromUri(selectedImageUri));
                            imageList.add(imageItem);
                            imageAdapter.notifyDataSetChanged();

                            updateImageUI();
                            Toast.makeText(getContext(), R.string.image_import_success, Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        // 初始化匹配图片选择器
        matchImagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                        selectedImageUri = result.getData().getData();
                        if (selectedImageUri != null) {
                            loadSelectedImage();
                        }
                    }
                });

        musicPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                        Uri selectedMusicUri = result.getData().getData();
                        if (selectedMusicUri != null) {
                            String fileName = ((MainActivity) getActivity()).getFileNameFromUri(selectedMusicUri);

                            MediaItem musicItem = new MediaItem();
                            musicItem.setUri(selectedMusicUri);
                            musicItem.setFileName(fileName);

                            // 尝试分离歌手名和歌曲名（通常格式为"歌手 - 歌曲名"）
                            if (fileName.contains(" - ")) {
                                String[] parts = fileName.split(" - ", 2);
                                musicItem.setArtistName(parts[0].trim());

                                // 移除文件扩展名
                                String songName = parts[1].trim();
                                int lastDot = songName.lastIndexOf(".");
                                if (lastDot > 0) {
                                    songName = songName.substring(0, lastDot);
                                }
                                musicItem.setSongName(songName);
                            } else {
                                // 如果没有特定格式，则使用文件名作为歌曲名
                                musicItem.setSongName(fileName);
                            }

                            // 随机生成关键词用于测试颜色匹配功能
                            generateRandomKeywords(musicItem);

                            musicList.add(musicItem);
                            musicAdapter.notifyDataSetChanged();

                            updateMusicUI();
                            Toast.makeText(getContext(), R.string.music_import_success, Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        midiPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                        Uri selectedMidiUri = result.getData().getData();
                        if (selectedMidiUri != null) {
                            String fileName = ((MainActivity) getActivity()).getFileNameFromUri(selectedMidiUri);

                            // 启动MIDI播放器
                            Intent playerIntent = new Intent(getActivity(), MidiPlayerActivity.class);
                            playerIntent.putExtra("midi_uri", selectedMidiUri);
                            playerIntent.putExtra("midi_filename", fileName);
                            startActivity(playerIntent);
                        }
                    }
                });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupRecyclerViews();
        setupListeners();

        // 同步全局列表
        syncWithGlobalLists();
    }

    private void syncWithGlobalLists() {
        // 从全局列表同步数据
        if (!MainActivity.globalImageList.isEmpty()) {
            imageList.clear();
            imageList.addAll(MainActivity.globalImageList);
            imageAdapter.notifyDataSetChanged();
            updateImageUI();
        }

        if (!MainActivity.globalMusicList.isEmpty()) {
            musicList.clear();
            musicList.addAll(MainActivity.globalMusicList);
            musicAdapter.notifyDataSetChanged();
            updateMusicUI();
        }
    }

    private void initViews(View view) {
        // 原有控件初始化
        imageRecyclerView = view.findViewById(R.id.imageRecyclerView);
        musicRecyclerView = view.findViewById(R.id.musicRecyclerView);
        addImageButton = view.findViewById(R.id.addImageButton);
        addMusicButton = view.findViewById(R.id.addMusicButton);
        viewAllImagesButton = view.findViewById(R.id.viewAllImagesButton);
        viewAllMusicButton = view.findViewById(R.id.viewAllMusicButton);
        importMidiButton = view.findViewById(R.id.importMidiButton);

        // 新控件初始化
        selectImageCard = view.findViewById(R.id.selectImageCard);
        selectedImageView = view.findViewById(R.id.selectedImageView);
        selectImageText = view.findViewById(R.id.selectImageText);
        matchButton = view.findViewById(R.id.matchButton);
        matchProgressBar = view.findViewById(R.id.matchProgressBar);
        matchPlaylistTitle = view.findViewById(R.id.matchPlaylistTitle);
        playMatchedPlaylistButton = view.findViewById(R.id.playMatchedPlaylistButton);
    }

    private void setupRecyclerViews() {
        // 设置图片RecyclerView
        imageAdapter = new MediaAdapter(getContext(), imageList, true);
        imageRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        imageRecyclerView.setAdapter(imageAdapter);

        // 设置音乐RecyclerView
        musicAdapter = new MusicAdapter(getContext(), musicList);
        musicRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        musicRecyclerView.setAdapter(musicAdapter);

        // 初始设置UI状态
        updateImageUI();
        updateMusicUI();
    }

    private void setupListeners() {
        // 图片导入按钮点击事件
        addImageButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        });

        // 音乐导入按钮点击事件
        addMusicButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("audio/*");  // 接受所有音频类型
            String[] mimeTypes = {"audio/mpeg", "audio/aac", "audio/flac", "audio/wav"};
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
            musicPickerLauncher.launch(intent);
        });

        // MIDI导入按钮点击事件
        importMidiButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");  // 允许所有文件类型
            String[] mimeTypes = {
                    "audio/midi",
                    "audio/x-midi",
                    "audio/mid",
                    "application/x-midi",
                    "application/midi"
            };
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
            midiPickerLauncher.launch(intent);
        });

        // 选择图片卡片点击事件
        selectImageCard.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            matchImagePickerLauncher.launch(intent);
        });

        // 匹配按钮点击事件
        matchButton.setOnClickListener(v -> {
            if (selectedImageUri == null) {
                Toast.makeText(getContext(), R.string.select_image_first, Toast.LENGTH_SHORT).show();
                return;
            }

            // 执行颜色匹配算法
            performColorMatching();
        });

        // 播放匹配歌单按钮点击事件
        playMatchedPlaylistButton.setOnClickListener(v -> {
            if (matchedMusicList.isEmpty()) {
                Toast.makeText(getContext(), R.string.no_matched_music, Toast.LENGTH_SHORT).show();
                return;
            }

            // 启动幻灯片播放界面
            Intent intent = new Intent(getActivity(), MatchedPlaylistActivity.class);
            intent.putExtra("image_uri", selectedImageUri.toString());
            startActivity(intent);
        });

        // 查看所有图片按钮点击事件
        viewAllImagesButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AllImagesActivity.class);
            startActivity(intent);
        });

        // 查看所有音乐按钮点击事件
        viewAllMusicButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AllMusicActivity.class);
            startActivity(intent);
        });
    }

    /**
     * 加载选中的图片
     */
    private void loadSelectedImage() {
        if (selectedImageUri != null) {
            try {
                // 使用Glide加载图片
                Glide.with(this)
                        .load(selectedImageUri)
                        .centerCrop()
                        .into(selectedImageView);

                // 显示选中的图片，隐藏提示文本
                selectedImageView.setVisibility(View.VISIBLE);
                selectImageText.setVisibility(View.GONE);

                // 从Uri加载Bitmap，用于颜色分析
                selectedImageBitmap = MediaStore.Images.Media.getBitmap(
                        getActivity().getContentResolver(), selectedImageUri);

            } catch (Exception e) {
                Log.e(TAG, "Error loading selected image", e);
                Toast.makeText(getContext(), R.string.error_loading_image, Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * 执行颜色匹配算法
     */
    private void performColorMatching() {
        if (selectedImageBitmap == null) {
            Toast.makeText(getContext(), R.string.error_processing_image, Toast.LENGTH_SHORT).show();
            return;
        }

        // 显示进度条，隐藏匹配按钮
        matchProgressBar.setVisibility(View.VISIBLE);
        matchButton.setVisibility(View.GONE);

        // 在后台线程中执行颜色提取和匹配
        new Thread(() -> {
            try {
                // 使用Palette API提取图片主色调
                Palette palette = Palette.from(selectedImageBitmap).generate();

                // 提取主要颜色
                int dominantColor = palette.getDominantColor(Color.BLACK);
                int vibrantColor = palette.getVibrantColor(Color.BLACK);
                int darkVibrantColor = palette.getDarkVibrantColor(Color.BLACK);
                int lightVibrantColor = palette.getLightVibrantColor(Color.BLACK);
                int mutedColor = palette.getMutedColor(Color.BLACK);

                // 构建颜色向量
                float[] imageColorVector = new float[] {
                        Color.red(dominantColor) / 255f,
                        Color.green(dominantColor) / 255f,
                        Color.blue(dominantColor) / 255f,
                        Color.red(vibrantColor) / 255f,
                        Color.green(vibrantColor) / 255f,
                        Color.blue(vibrantColor) / 255f,
                        Color.red(darkVibrantColor) / 255f,
                        Color.green(darkVibrantColor) / 255f,
                        Color.blue(darkVibrantColor) / 255f,
                        Color.red(lightVibrantColor) / 255f,
                        Color.green(lightVibrantColor) / 255f,
                        Color.blue(lightVibrantColor) / 255f,
                        Color.red(mutedColor) / 255f,
                        Color.green(mutedColor) / 255f,
                        Color.blue(mutedColor) / 255f
                };

                // 调用服务器端进行匹配
                matchMusicWithColors(imageColorVector);

            } catch (Exception e) {
                Log.e(TAG, "Color matching error", e);
                getActivity().runOnUiThread(() -> {
                    matchProgressBar.setVisibility(View.GONE);
                    matchButton.setVisibility(View.VISIBLE);
                    Toast.makeText(getContext(), R.string.color_matching_error, Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    /**
     * 用颜色向量匹配音乐
     * 注意：实际项目中，这里应该调用后端API，但为了演示，我们在本地处理
     */
    private void matchMusicWithColors(float[] colorVector) {
        // 模拟网络延迟
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // 创建匹配的音乐列表
        List<MediaItemMatch> matches = new ArrayList<>();

        // 假设每首歌都有关键词，计算与图片颜色的匹配度
        for (MediaItem musicItem : musicList) {
            // 获取歌曲关键词
            String[] keywords = getKeywords(musicItem);
            if (keywords == null || keywords.length == 0) {
                continue;
            }

            // 计算关键词与颜色的匹配度
            float matchScore = calculateMatchScore(keywords, colorVector);

            // 将匹配结果添加到列表
            matches.add(new MediaItemMatch(musicItem, matchScore));
        }

        // 根据匹配度排序
        Collections.sort(matches, (a, b) ->
                Float.compare(b.matchScore, a.matchScore));

        // 取前5个最匹配的音乐
        final List<MediaItem> topMatches = new ArrayList<>();
        final int maxMatches = Math.min(5, matches.size());
        for (int i = 0; i < maxMatches; i++) {
            topMatches.add(matches.get(i).mediaItem);
        }

        // 将匹配结果保存到全局变量
        MainActivity.matchedImageUri = selectedImageUri;
        MainActivity.matchedMusicList.clear();
        MainActivity.matchedMusicList.addAll(topMatches);

        // 在UI线程更新UI
        getActivity().runOnUiThread(() -> {
            // 显示匹配结果
            showMatchedResults(topMatches);

            // 隐藏进度条，显示匹配按钮
            matchProgressBar.setVisibility(View.GONE);
            matchButton.setVisibility(View.VISIBLE);
        });
    }

    /**
     * 显示匹配结果
     */
    private void showMatchedResults(List<MediaItem> matchedMusic) {
        if (matchedMusic.isEmpty()) {
            Toast.makeText(getContext(), R.string.no_music_matched, Toast.LENGTH_SHORT).show();
            return;
        }

        // 保存匹配的音乐列表
        matchedMusicList.clear();
        matchedMusicList.addAll(matchedMusic);

        // 更新音乐RecyclerView显示匹配的音乐
        musicAdapter = new MusicAdapter(getContext(), matchedMusic);
        musicRecyclerView.setAdapter(musicAdapter);

        // 显示匹配播放列表标题和播放按钮
        matchPlaylistTitle.setVisibility(View.VISIBLE);
        playMatchedPlaylistButton.setVisibility(View.VISIBLE);
        addMusicButton.setVisibility(View.GONE);

        // 切换到匹配模式
        isMatchMode = true;

        // 提示用户
        Toast.makeText(getContext(), R.string.music_matched_success, Toast.LENGTH_SHORT).show();
    }

    /**
     * 获取音乐项的关键词
     */
    private String[] getKeywords(MediaItem musicItem) {
        // 实际项目中，这些关键词应该从数据库或API获取
        // 此处为了演示，我们使用随机关键词
        if (musicItem.getKeywords() == null || musicItem.getKeywords().isEmpty()) {
            generateRandomKeywords(musicItem);
        }

        return musicItem.getKeywords().split(",");
    }

    /**
     * 生成随机关键词，用于测试
     */
    private void generateRandomKeywords(MediaItem musicItem) {
        // 关键词库
        String[] allKeywords = {
                "快乐", "悲伤", "激动", "平静", "忧郁", "兴奋", "温暖", "冷淡",
                "明亮", "黑暗", "活力", "疲惫", "热情", "冷静", "柔和", "强烈",
                "轻快", "沉重", "清新", "浑浊", "甜蜜", "苦涩", "欢快", "忧伤",
                "阳光", "阴雨", "彩虹", "灰暗", "春天", "夏天", "秋天", "冬天"
        };

        // 随机选择3-5个关键词
        int keywordCount = 3 + (int)(Math.random() * 3);
        StringBuilder keywords = new StringBuilder();

        for (int i = 0; i < keywordCount; i++) {
            int index = (int)(Math.random() * allKeywords.length);
            if (i > 0) keywords.append(",");
            keywords.append(allKeywords[index]);
        }

        // 保存关键词
        musicItem.setKeywords(keywords.toString());
    }

    /**
     * 计算关键词与颜色的匹配度
     */
    private float calculateMatchScore(String[] keywords, float[] colorVector) {
        // 关键词到颜色的映射表
        Map<String, float[]> keywordColorMap = getKeywordColorMap();

        float totalScore = 0;
        int matchedKeywords = 0;

        // 对每个关键词计算匹配度
        for (String keyword : keywords) {
            if (keywordColorMap.containsKey(keyword)) {
                float[] keywordColor = keywordColorMap.get(keyword);
                float similarity = calculateColorSimilarity(keywordColor, colorVector);
                totalScore += similarity;
                matchedKeywords++;
            }
        }

        // 如果没有匹配的关键词，返回随机值
        if (matchedKeywords == 0) {
            return (float) Math.random();
        }

        // 返回平均匹配度
        return totalScore / matchedKeywords;
    }

    /**
     * 计算两个颜色向量的相似度 (余弦相似度)
     */
    private float calculateColorSimilarity(float[] color1, float[] color2) {
        // 简化版：只比较前三个值 (RGB)
        float dotProduct = 0;
        float norm1 = 0;
        float norm2 = 0;

        for (int i = 0; i < 3; i++) {
            dotProduct += color1[i] * color2[i];
            norm1 += color1[i] * color1[i];
            norm2 += color2[i] * color2[i];
        }

        // 避免除零错误
        if (norm1 == 0 || norm2 == 0) {
            return 0;
        }

        return dotProduct / (float)(Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    /**
     * 获取关键词到颜色的映射表
     */
    private Map<String, float[]> getKeywordColorMap() {
        Map<String, float[]> map = new HashMap<>();

        // 关键词到颜色的映射
        map.put("快乐", new float[]{1.0f, 1.0f, 0.0f}); // 黄色
        map.put("悲伤", new float[]{0.0f, 0.0f, 0.8f}); // 蓝色
        map.put("激动", new float[]{1.0f, 0.0f, 0.0f}); // 红色
        map.put("平静", new float[]{0.5f, 0.7f, 1.0f}); // 淡蓝色
        map.put("忧郁", new float[]{0.5f, 0.5f, 0.7f}); // 灰蓝色
        map.put("兴奋", new float[]{1.0f, 0.5f, 0.0f}); // 橙色
        map.put("温暖", new float[]{1.0f, 0.8f, 0.6f}); // 暖色
        map.put("冷淡", new float[]{0.6f, 0.8f, 0.8f}); // 冷色
        map.put("明亮", new float[]{1.0f, 1.0f, 0.8f}); // 亮色
        map.put("黑暗", new float[]{0.2f, 0.2f, 0.2f}); // 暗色
        map.put("活力", new float[]{0.8f, 0.2f, 0.8f}); // 紫色
        map.put("疲惫", new float[]{0.5f, 0.5f, 0.5f}); // 灰色
        map.put("热情", new float[]{1.0f, 0.2f, 0.2f}); // 红色
        map.put("冷静", new float[]{0.0f, 0.5f, 0.5f}); // 青色
        map.put("柔和", new float[]{0.8f, 0.8f, 1.0f}); // 淡紫色
        map.put("强烈", new float[]{0.9f, 0.1f, 0.1f}); // 深红色
        map.put("轻快", new float[]{0.7f, 1.0f, 0.7f}); // 淡绿色
        map.put("沉重", new float[]{0.3f, 0.3f, 0.4f}); // 深灰色
        map.put("清新", new float[]{0.4f, 0.8f, 0.4f}); // 绿色
        map.put("浑浊", new float[]{0.5f, 0.4f, 0.3f}); // 棕色
        map.put("甜蜜", new float[]{1.0f, 0.7f, 0.7f}); // 粉色
        map.put("苦涩", new float[]{0.3f, 0.2f, 0.1f}); // 深棕色
        map.put("欢快", new float[]{0.9f, 0.9f, 0.0f}); // 黄色
        map.put("忧伤", new float[]{0.1f, 0.3f, 0.6f}); // 灰蓝色
        map.put("阳光", new float[]{1.0f, 0.9f, 0.5f}); // 暖黄色
        map.put("阴雨", new float[]{0.5f, 0.5f, 0.6f}); // 灰色
        map.put("彩虹", new float[]{0.6f, 0.0f, 0.6f}); // 紫色
        map.put("灰暗", new float[]{0.4f, 0.4f, 0.4f}); // 灰色
        map.put("春天", new float[]{0.7f, 0.9f, 0.5f}); // 嫩绿色
        map.put("夏天", new float[]{0.0f, 0.8f, 1.0f}); // 蓝绿色
        map.put("秋天", new float[]{0.8f, 0.5f, 0.2f}); // 橙褐色
        map.put("冬天", new float[]{0.9f, 0.9f, 0.9f}); // 白色

        return map;
    }

    // 添加管理UI显示状态的方法
    private void updateImageUI() {
        if (imageList.isEmpty()) {
            imageRecyclerView.setVisibility(View.GONE);
            addImageButton.setVisibility(View.VISIBLE);
            viewAllImagesButton.setVisibility(View.GONE);
        } else {
            imageRecyclerView.setVisibility(View.VISIBLE);
            viewAllImagesButton.setVisibility(View.VISIBLE);

            // 当图片数量达到最大显示数量时，隐藏添加按钮
            addImageButton.setVisibility(imageList.size() >= MAX_VISIBLE_IMAGES ? View.GONE : View.VISIBLE);
        }

        // 同步到全局列表
        MainActivity.globalImageList.clear();
        MainActivity.globalImageList.addAll(imageList);
    }

    private void updateMusicUI() {
        if (isMatchMode) {
            // 匹配模式下，显示匹配的音乐
            if (matchedMusicList.isEmpty()) {
                musicRecyclerView.setVisibility(View.GONE);
            } else {
                musicRecyclerView.setVisibility(View.VISIBLE);
            }

            addMusicButton.setVisibility(View.GONE);
            viewAllMusicButton.setVisibility(View.VISIBLE);
            matchPlaylistTitle.setVisibility(View.VISIBLE);
            playMatchedPlaylistButton.setVisibility(View.VISIBLE);

        } else {
            // 普通模式
            if (musicList.isEmpty()) {
                musicRecyclerView.setVisibility(View.GONE);
                addMusicButton.setVisibility(View.VISIBLE);
                viewAllMusicButton.setVisibility(View.GONE);
            } else {
                musicRecyclerView.setVisibility(View.VISIBLE);
                viewAllMusicButton.setVisibility(View.VISIBLE);

                // 当音乐数量达到最大显示数量时，隐藏添加按钮
                addMusicButton.setVisibility(musicList.size() >= MAX_VISIBLE_MUSIC ? View.GONE : View.VISIBLE);
            }

            matchPlaylistTitle.setVisibility(View.GONE);
            playMatchedPlaylistButton.setVisibility(View.GONE);
        }

        // 同步到全局列表
        MainActivity.globalMusicList.clear();
        MainActivity.globalMusicList.addAll(musicList);
    }

    /**
     * 切换回普通模式
     */
    private void resetToNormalMode() {
        isMatchMode = false;
        selectedImageUri = null;
        selectedImageBitmap = null;

        // 重置UI
        selectedImageView.setVisibility(View.INVISIBLE);
        selectImageText.setVisibility(View.VISIBLE);

        // 更新音乐列表
        musicAdapter = new MusicAdapter(getContext(), musicList);
        musicRecyclerView.setAdapter(musicAdapter);

        // 更新UI状态
        updateMusicUI();
    }

    /**
     * 用于匹配的媒体项类
     */
    private static class MediaItemMatch {
        MediaItem mediaItem;
        float matchScore;

        public MediaItemMatch(MediaItem mediaItem, float matchScore) {
            this.mediaItem = mediaItem;
            this.matchScore = matchScore;
        }
    }
}