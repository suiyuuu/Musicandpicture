package com.example.musicandpicture;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

public class CommunityFragment extends Fragment {

    private TabLayout communityTabLayout;
    private RecyclerView communityRecyclerView;
    private ExtendedFloatingActionButton shareNewContentButton;

    private List<ShareItem> popularItems = new ArrayList<>();
    private List<ShareItem> latestItems = new ArrayList<>();
    private List<ShareItem> myShareItems = new ArrayList<>();

    private CommunityAdapter adapter;
    private int currentTabPosition = 0;

    public CommunityFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_community, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupRecyclerView();
        setupListeners();

        // 加载模拟数据
        loadDummyData();
    }

    private void initViews(View view) {
        communityTabLayout = view.findViewById(R.id.communityTabLayout);
        communityRecyclerView = view.findViewById(R.id.communityRecyclerView);
        shareNewContentButton = view.findViewById(R.id.shareNewContentButton);
    }

    private void setupRecyclerView() {
        adapter = new CommunityAdapter(getContext(), popularItems);
        communityRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        communityRecyclerView.setAdapter(adapter);
    }

    private void setupListeners() {
        // Tab选择监听
        communityTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTabPosition = tab.getPosition();
                updateTabContent();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // 不需要操作
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // 不需要操作
            }
        });

        // 分享新内容按钮点击事件
        shareNewContentButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ShareContentActivity.class);
            startActivity(intent);
        });
    }

    private void updateTabContent() {
        switch (currentTabPosition) {
            case 0: // 热门
                adapter.updateItems(popularItems);
                break;
            case 1: // 最新
                adapter.updateItems(latestItems);
                break;
            case 2: // 我的分享
                adapter.updateItems(myShareItems);
                break;
        }
    }

    private void loadDummyData() {
        // 添加一些模拟数据用于显示

        // 热门内容
        popularItems.add(new ShareItem(
                "音乐与回忆",
                "张三",
                "这首歌让我想起了高中时代的美好记忆...",
                System.currentTimeMillis() - 3 * 24 * 60 * 60 * 1000,
                128,
                R.drawable.dummy_image_1
        ));

        popularItems.add(new ShareItem(
                "城市夜景",
                "李四",
                "夜晚的城市灯光总是那么迷人，配上这首歌更是绝配！",
                System.currentTimeMillis() - 5 * 24 * 60 * 60 * 1000,
                97,
                R.drawable.dummy_image_2
        ));

        // 最新内容
        latestItems.add(new ShareItem(
                "春天的旋律",
                "王五",
                "春暖花开，万物复苏，这首歌正好表达了我的心情。",
                System.currentTimeMillis() - 6 * 60 * 60 * 1000,
                12,
                R.drawable.dummy_image_3
        ));

        latestItems.add(new ShareItem(
                "雨天的心情",
                "赵六",
                "下雨天总是让人感到些许忧伤，听着这首歌，看着窗外的雨滴...",
                System.currentTimeMillis() - 12 * 60 * 60 * 1000,
                8,
                R.drawable.dummy_image_4
        ));

        // 我的分享 (留空，实际使用中应从数据库加载用户的分享)
    }

    @Override
    public void onResume() {
        super.onResume();
        // 可以在这里刷新数据，例如从服务器获取最新数据
    }
}