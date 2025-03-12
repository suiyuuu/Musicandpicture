package com.example.musicandpicture;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;

import com.google.android.flexbox.FlexboxLayout;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class KeywordsFragment extends Fragment {

    private static final String TAG = "KeywordsFragment";
    private static final int MAX_KEYWORDS = 8; // 最大允许选择的关键词数量

    private FlexboxLayout keywordsContainer;
    private Button generateStoryButton;
    private Button addKeywordButton;
    private EditText customKeywordEditText;
    private ProgressBar keywordsProgressBar;
    private TextView keywordsErrorTextView;
    private TextView selectedCountTextView;

    private Set<String> selectedKeywords = new HashSet<>();
    private List<String> availableKeywords = new ArrayList<>();
    private OnGenerateStoryListener storyListener;

    public interface OnGenerateStoryListener {
        void onGenerateStory(List<String> keywords);
    }

    public KeywordsFragment() {
        // 必需的空构造函数
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            storyListener = (OnGenerateStoryListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnGenerateStoryListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_keywords, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 初始化视图
        keywordsContainer = view.findViewById(R.id.keywordsContainer);
        generateStoryButton = view.findViewById(R.id.generateStoryButton);
        addKeywordButton = view.findViewById(R.id.addKeywordButton);
        customKeywordEditText = view.findViewById(R.id.customKeywordEditText);
        keywordsProgressBar = view.findViewById(R.id.keywordsProgressBar);
        keywordsErrorTextView = view.findViewById(R.id.keywordsErrorTextView);
        selectedCountTextView = view.findViewById(R.id.selectedCountTextView);

        // 设置生成故事按钮点击事件
        generateStoryButton.setOnClickListener(v -> {
            if (selectedKeywords.isEmpty()) {
                Toast.makeText(getContext(), R.string.select_keywords_prompt, Toast.LENGTH_SHORT).show();
            } else {
                storyListener.onGenerateStory(new ArrayList<>(selectedKeywords));
            }
        });

        // 设置添加自定义关键词按钮
        addKeywordButton.setOnClickListener(v -> {
            String keyword = customKeywordEditText.getText().toString().trim();
            if (!keyword.isEmpty()) {
                addCustomKeyword(keyword);
                customKeywordEditText.setText("");
            } else {
                Toast.makeText(getContext(), R.string.keyword_cannot_be_empty, Toast.LENGTH_SHORT).show();
            }
        });

        // 初始状态为加载中
        showLoading();
    }

    private void addCustomKeyword(String keyword) {
        // 检查是否已经包含这个关键词
        if (availableKeywords.contains(keyword)) {
            Toast.makeText(getContext(), R.string.keyword_already_exists, Toast.LENGTH_SHORT).show();
            return;
        }

        // 添加到可用关键词并创建一个按钮
        availableKeywords.add(keyword);
        addKeywordButton(keyword, true); // 添加并默认选中
    }

    /**
     * 显示加载中状态
     */
    public void showLoading() {
        if (keywordsProgressBar != null) {
            keywordsProgressBar.setVisibility(View.VISIBLE);
        }
        if (keywordsContainer != null) {
            keywordsContainer.setVisibility(View.GONE);
        }
        if (generateStoryButton != null) {
            generateStoryButton.setVisibility(View.GONE);
        }
        if (keywordsErrorTextView != null) {
            keywordsErrorTextView.setVisibility(View.GONE);
        }
        if (customKeywordEditText != null) {
            customKeywordEditText.setVisibility(View.GONE);
        }
        if (addKeywordButton != null) {
            addKeywordButton.setVisibility(View.GONE);
        }
        if (selectedCountTextView != null) {
            selectedCountTextView.setVisibility(View.GONE);
        }
    }

    /**
     * 显示错误状态
     */
    public void showError() {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (keywordsProgressBar != null) {
                    keywordsProgressBar.setVisibility(View.GONE);
                }
                if (keywordsContainer != null) {
                    keywordsContainer.setVisibility(View.GONE);
                }
                if (generateStoryButton != null) {
                    generateStoryButton.setVisibility(View.GONE);
                }
                if (keywordsErrorTextView != null) {
                    keywordsErrorTextView.setVisibility(View.VISIBLE);
                    keywordsErrorTextView.setText(R.string.keywords_not_found);
                }
                if (customKeywordEditText != null) {
                    customKeywordEditText.setVisibility(View.GONE);
                }
                if (addKeywordButton != null) {
                    addKeywordButton.setVisibility(View.GONE);
                }
                if (selectedCountTextView != null) {
                    selectedCountTextView.setVisibility(View.GONE);
                }
            });
        }
    }

    /**
     * 更新关键词列表
     * @param keywords 关键词数组
     */
    public void updateKeywords(String[] keywords) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                try {
                    // 清空所有数据
                    availableKeywords.clear();
                    selectedKeywords.clear();

                    if (keywordsProgressBar != null) {
                        keywordsProgressBar.setVisibility(View.GONE);
                    }
                    if (keywordsErrorTextView != null) {
                        keywordsErrorTextView.setVisibility(View.GONE);
                    }

                    if (keywordsContainer != null) {
                        keywordsContainer.removeAllViews();
                        keywordsContainer.setVisibility(View.VISIBLE);

                        // 创建关键词按钮
                        if (keywords != null && keywords.length > 0) {
                            for (String keyword : keywords) {
                                if (keyword != null && !keyword.trim().isEmpty()) {
                                    availableKeywords.add(keyword);
                                    addKeywordButton(keyword, false);
                                }
                            }
                        } else {
                            // 添加一个"无关键词"的提示
                            TextView noKeywordsText = new TextView(getContext());
                            noKeywordsText.setText(R.string.no_keywords_found);
                            noKeywordsText.setTextColor(ContextCompat.getColor(getContext(), android.R.color.white));
                            keywordsContainer.addView(noKeywordsText);
                        }
                    }

                    // 显示表单控件
                    if (generateStoryButton != null) {
                        generateStoryButton.setVisibility(View.VISIBLE);
                    }
                    if (customKeywordEditText != null) {
                        customKeywordEditText.setVisibility(View.VISIBLE);
                    }
                    if (addKeywordButton != null) {
                        addKeywordButton.setVisibility(View.VISIBLE);
                    }

                    updateSelectedCount();

                } catch (Exception e) {
                    Log.e(TAG, "更新关键词出错: " + e.getMessage());
                    e.printStackTrace();
                    showError();
                }
            });
        }
    }

    /**
     * 添加关键词按钮
     * @param keyword 关键词
     * @param selected 是否默认选中
     */
    private void addKeywordButton(String keyword, boolean selected) {
        if (getContext() == null || keywordsContainer == null) return;

        try {
            // 创建按钮
            Button keywordButton = new Button(getContext());
            keywordButton.setText(keyword);
            keywordButton.setAllCaps(false); // 不自动转大写

            // 设置初始状态
            if (selected) {
                selectedKeywords.add(keyword);
                keywordButton.setBackgroundResource(R.drawable.keyword_button_selected_background);
                keywordButton.setTextColor(ContextCompat.getColor(getContext(), R.color.keyword_selected_text_color));
            } else {
                keywordButton.setBackgroundResource(R.drawable.keyword_button_background);
                keywordButton.setTextColor(ContextCompat.getColor(getContext(), R.color.keyword_text_color));
            }

            keywordButton.setPadding(24, 12, 24, 12);

            // 设置布局参数
            FlexboxLayout.LayoutParams params = new FlexboxLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(8, 8, 8, 8);
            keywordButton.setLayoutParams(params);

            // 设置点击事件
            keywordButton.setOnClickListener(v -> {
                if (selectedKeywords.contains(keyword)) {
                    // 取消选择
                    selectedKeywords.remove(keyword);
                    keywordButton.setBackgroundResource(R.drawable.keyword_button_background);
                    keywordButton.setTextColor(ContextCompat.getColor(getContext(), R.color.keyword_text_color));
                } else {
                    // 检查是否超过最大选择数量
                    if (selectedKeywords.size() >= MAX_KEYWORDS) {
                        Toast.makeText(getContext(),
                                getString(R.string.max_keywords_limit, MAX_KEYWORDS),
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // 选择关键词
                    selectedKeywords.add(keyword);
                    keywordButton.setBackgroundResource(R.drawable.keyword_button_selected_background);
                    keywordButton.setTextColor(ContextCompat.getColor(getContext(), R.color.keyword_selected_text_color));
                }

                updateSelectedCount();
            });

            // 添加到容器
            keywordsContainer.addView(keywordButton);

            // 更新计数
            updateSelectedCount();

        } catch (Exception e) {
            Log.e(TAG, "添加关键词按钮失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 更新已选择关键词的计数
     */
    private void updateSelectedCount() {
        if (selectedCountTextView != null) {
            if (selectedKeywords.isEmpty()) {
                selectedCountTextView.setVisibility(View.GONE);
            } else {
                selectedCountTextView.setVisibility(View.VISIBLE);
                selectedCountTextView.setText(getString(R.string.selected_keywords_count,
                        selectedKeywords.size(), MAX_KEYWORDS));
            }
        }
    }

    /**
     * 清除所有选中的关键词
     */
    public void clearSelectedKeywords() {
        selectedKeywords.clear();
        updateKeywords(availableKeywords.toArray(new String[0]));
    }
}