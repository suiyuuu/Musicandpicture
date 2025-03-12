package com.example.musicandpicture;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private static final int MAX_VISIBLE_IMAGES = 6;
    private static final int MAX_VISIBLE_MUSIC = 5;

    private RecyclerView imageRecyclerView;
    private RecyclerView musicRecyclerView;
    private FloatingActionButton addImageButton;
    private FloatingActionButton addMusicButton;
    private Button viewAllImagesButton;
    private Button viewAllMusicButton;
    private Button importMidiButton;

    private List<MediaItem> imageList = new ArrayList<>();
    private List<MediaItem> musicList = new ArrayList<>();
    private MediaAdapter imageAdapter;
    private MusicAdapter musicAdapter;

    // 使用 ActivityResultLauncher 处理图片选择结果
    private ActivityResultLauncher<Intent> imagePickerLauncher;

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
        imageRecyclerView = view.findViewById(R.id.imageRecyclerView);
        musicRecyclerView = view.findViewById(R.id.musicRecyclerView);
        addImageButton = view.findViewById(R.id.addImageButton);
        addMusicButton = view.findViewById(R.id.addMusicButton);
        viewAllImagesButton = view.findViewById(R.id.viewAllImagesButton);
        viewAllMusicButton = view.findViewById(R.id.viewAllMusicButton);
        importMidiButton = view.findViewById(R.id.importMidiButton);
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

        // 同步到全局列表
        MainActivity.globalMusicList.clear();
        MainActivity.globalMusicList.addAll(musicList);
    }
}