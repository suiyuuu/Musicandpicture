package com.example.musicandpicture;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.tabs.TabLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CommunityFragment extends Fragment {

    private static final String TAG = "CommunityFragment";

    private TabLayout communityTabLayout;
    private RecyclerView communityRecyclerView;
    private ExtendedFloatingActionButton shareNewContentButton;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView emptyStateTextView;

    private List<ShareItem> popularItems = new ArrayList<>();
    private List<ShareItem> latestItems = new ArrayList<>();
    private List<ShareItem> myShareItems = new ArrayList<>();

    private CommunityAdapter adapter;
    private int currentTabPosition = 0;
    private String deviceId;

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

        // 获取设备ID用于识别用户自己的分享
        deviceId = Settings.Secure.getString(requireActivity().getContentResolver(),
                Settings.Secure.ANDROID_ID);

        initViews(view);
        setupRecyclerView();
        setupListeners();

        // 加载分享数据
        loadShareItems();
    }

    private void initViews(View view) {
        communityTabLayout = view.findViewById(R.id.communityTabLayout);
        communityRecyclerView = view.findViewById(R.id.communityRecyclerView);
        shareNewContentButton = view.findViewById(R.id.shareNewContentButton);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        emptyStateTextView = view.findViewById(R.id.emptyStateTextView);
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

        // 下拉刷新监听
        swipeRefreshLayout.setOnRefreshListener(this::loadShareItems);

        // 设置刷新控件的颜色
        swipeRefreshLayout.setColorSchemeResources(
                R.color.colorPrimary,
                R.color.colorAccent,
                R.color.teal_200
        );
    }

    @Override
    public void onResume() {
        super.onResume();
        // 每次恢复可见状态时刷新数据
        loadShareItems();
    }

    private void loadShareItems() {
        // 显示刷新动画
        swipeRefreshLayout.setRefreshing(true);

        // 清空现有数据
        popularItems.clear();
        latestItems.clear();
        myShareItems.clear();

        // 从SharedPreferences加载分享项
        SharedPreferences prefs = requireActivity().getSharedPreferences("ShareItems", Context.MODE_PRIVATE);
        String itemListJson = prefs.getString("itemList", "[]");

        try {
            JSONArray itemList = new JSONArray(itemListJson);

            // 如果没有分享项，显示空状态
            if (itemList.length() == 0) {
                showEmptyState(true);
                swipeRefreshLayout.setRefreshing(false);
                return;
            }

            // 隐藏空状态
            showEmptyState(false);

            // 加载所有分享项
            for (int i = 0; i < itemList.length(); i++) {
                String itemId = itemList.getString(i);
                String itemJson = prefs.getString(itemId, null);

                if (itemJson != null) {
                    try {
                        JSONObject itemObject = new JSONObject(itemJson);

                        // 创建ShareItem对象
                        ShareItem item = new ShareItem();
                        item.setId(itemObject.getString("id"));
                        item.setTitle(itemObject.getString("title"));
                        item.setAuthor(itemObject.getString("author"));
                        item.setAuthorId(itemObject.getString("authorId"));
                        item.setContent(itemObject.getString("content"));
                        item.setImagePath(itemObject.getString("imagePath"));
                        item.setTimestamp(itemObject.getLong("timestamp"));
                        item.setLikes(itemObject.getInt("likes"));

                        // 可选字段：音乐URI
                        if (itemObject.has("musicUri")) {
                            item.setAudioUri(Uri.parse(itemObject.getString("musicUri")));
                        }

                        // 读取图片
                        File imageFile = new File(item.getImagePath());
                        if (imageFile.exists()) {
                            Bitmap bitmap = BitmapFactory.decodeFile(item.getImagePath());
                            item.setImage(bitmap);
                        }

                        // 读取标签
                        if (itemObject.has("tags")) {
                            JSONArray tagsArray = itemObject.getJSONArray("tags");
                            List<String> tags = new ArrayList<>();
                            for (int j = 0; j < tagsArray.length(); j++) {
                                tags.add(tagsArray.getString(j));
                            }
                            item.setTags(tags);
                        }

                        // 添加到相应的列表
                        latestItems.add(item);

                        // 如果是自己的分享，添加到"我的分享"列表
                        if (item.getAuthorId().equals(deviceId)) {
                            myShareItems.add(item);
                        }

                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing share item JSON: " + e.getMessage());
                    }
                }
            }

            // 按时间排序（最新的在前面）
            Collections.sort(latestItems, (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));

            // 复制到热门列表并按点赞数排序
            popularItems.addAll(latestItems);
            Collections.sort(popularItems, (a, b) -> Integer.compare(b.getLikes(), a.getLikes()));

            // 更新当前选项卡内容
            updateTabContent();

        } catch (JSONException e) {
            Log.e(TAG, "Error parsing item list JSON: " + e.getMessage());
        } finally {
            // 停止刷新动画
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    private void showEmptyState(boolean show) {
        if (show) {
            emptyStateTextView.setVisibility(View.VISIBLE);
            communityRecyclerView.setVisibility(View.GONE);
        } else {
            emptyStateTextView.setVisibility(View.GONE);
            communityRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void updateTabContent() {
        switch (currentTabPosition) {
            case 0: // 热门
                adapter.updateItems(popularItems);
                showEmptyState(popularItems.isEmpty());
                break;
            case 1: // 最新
                adapter.updateItems(latestItems);
                showEmptyState(latestItems.isEmpty());
                break;
            case 2: // 我的分享
                adapter.updateItems(myShareItems);
                showEmptyState(myShareItems.isEmpty());
                if (myShareItems.isEmpty()) {
                    emptyStateTextView.setText(R.string.no_personal_shares);
                }
                break;
        }
    }
}