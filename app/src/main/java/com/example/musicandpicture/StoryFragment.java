package com.example.musicandpicture;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
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

import com.example.musicandpicture.database.AppDatabase;
import com.example.musicandpicture.database.StoryEntity;
import java.util.List;

public class StoryFragment extends Fragment {

    private static final String TAG = "StoryFragment";
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private EditText storyEditText;
    private Button saveStoryButton;
    private ProgressBar storyProgressBar;
    private TextView storyErrorTextView;

    private String currentSongUri = "";
    private String currentStoryContent = "";
    private String currentKeywords = "";

    // 保存当前Context以防Activity为null
    private Context safeContext;

    public StoryFragment() {
        // 必需的空构造函数
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // 保存Context引用
        safeContext = context;
        Log.d(TAG, "onAttach: 片段已附加到Context");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: 创建故事片段视图");
        return inflater.inflate(R.layout.fragment_story, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated: 故事片段视图已创建");

        try {
            // 初始化视图
            storyEditText = view.findViewById(R.id.storyEditText);
            saveStoryButton = view.findViewById(R.id.saveStoryButton);
            storyProgressBar = view.findViewById(R.id.storyProgressBar);
            storyErrorTextView = view.findViewById(R.id.storyErrorTextView);

            // 确保EditText可编辑
            storyEditText.setEnabled(true);
            storyEditText.setFocusable(true);
            storyEditText.setFocusableInTouchMode(true);

            // 如果有保存的内容，恢复它
            if (!TextUtils.isEmpty(currentStoryContent)) {
                storyEditText.setText(currentStoryContent);
            }

            // 设置保存故事按钮点击事件
            if (saveStoryButton != null) {
                saveStoryButton.setOnClickListener(v -> {
                    Log.d(TAG, "onClick: 点击保存故事按钮");
                    saveStory();
                });
            }

            // 初始状态为等待生成故事
            showWaiting();
        } catch (Exception e) {
            Log.e(TAG, "onViewCreated: 初始化视图出错", e);
        }
    }

    /**
     * 显示等待状态
     */
    public void showWaiting() {
        Log.d(TAG, "showWaiting: 显示等待状态");
        runOnUiThread(() -> {
            try {
                if (storyProgressBar != null) {
                    storyProgressBar.setVisibility(View.GONE);
                }
                if (storyEditText != null) {
                    storyEditText.setVisibility(View.VISIBLE);
                    storyEditText.setHint(R.string.story_waiting_hint);
                    storyEditText.setEnabled(true);
                    storyEditText.setFocusable(true);
                    storyEditText.setFocusableInTouchMode(true);

                    // 恢复之前的内容
                    if (!TextUtils.isEmpty(currentStoryContent)) {
                        storyEditText.setText(currentStoryContent);
                    }
                }
                if (saveStoryButton != null) {
                    saveStoryButton.setVisibility(View.VISIBLE);
                }
                if (storyErrorTextView != null) {
                    storyErrorTextView.setVisibility(View.GONE);
                }
            } catch (Exception e) {
                Log.e(TAG, "showWaiting: 显示等待状态出错", e);
            }
        });
    }

    /**
     * 显示加载中状态
     */
    public void showLoading() {
        Log.d(TAG, "showLoading: 显示加载状态");
        runOnUiThread(() -> {
            try {
                if (storyProgressBar != null) {
                    storyProgressBar.setVisibility(View.VISIBLE);
                }
                if (storyEditText != null) {
                    storyEditText.setVisibility(View.GONE);
                }
                if (saveStoryButton != null) {
                    saveStoryButton.setVisibility(View.GONE);
                }
                if (storyErrorTextView != null) {
                    storyErrorTextView.setVisibility(View.GONE);
                }
            } catch (Exception e) {
                Log.e(TAG, "showLoading: 显示加载状态出错", e);
            }
        });
    }

    /**
     * 显示错误状态
     */
    public void showError() {
        Log.d(TAG, "showError: 显示错误状态");
        runOnUiThread(() -> {
            try {
                if (storyProgressBar != null) {
                    storyProgressBar.setVisibility(View.GONE);
                }

                // 不完全隐藏编辑界面，而是仍然允许用户自行创建故事
                if (storyEditText != null) {
                    storyEditText.setVisibility(View.VISIBLE);
                    storyEditText.setText("故事生成失败，您可以在此创建自己的故事...");
                    storyEditText.setEnabled(true);
                    storyEditText.setFocusable(true);
                    storyEditText.setFocusableInTouchMode(true);
                    currentStoryContent = "故事生成失败，您可以在此创建自己的故事...";
                }

                if (saveStoryButton != null) {
                    saveStoryButton.setVisibility(View.VISIBLE);
                }

                if (storyErrorTextView != null) {
                    storyErrorTextView.setVisibility(View.VISIBLE);
                    storyErrorTextView.setText("无法从服务器获取故事，但您可以创建自己的故事");
                }
            } catch (Exception e) {
                Log.e(TAG, "showError: 显示错误状态出错", e);
            }
        });
    }

    /**
     * 更新故事内容
     * @param songUri 歌曲URI
     * @param storyContent 故事内容
     * @param keywords 用于生成故事的关键词
     */
    public void updateStory(String songUri, String storyContent, String keywords) {
        Log.d(TAG, "updateStory: 更新故事内容, URI: " + songUri + ", 内容长度: " +
                (storyContent != null ? storyContent.length() : 0));

        // 即使Activity为null也保存这些值
        currentSongUri = songUri != null ? songUri : "";
        currentStoryContent = storyContent != null ? storyContent : "";
        currentKeywords = keywords != null ? keywords : "";

        runOnUiThread(() -> {
            try {
                if (storyProgressBar != null) {
                    storyProgressBar.setVisibility(View.GONE);
                }
                if (storyErrorTextView != null) {
                    storyErrorTextView.setVisibility(View.GONE);
                }
                if (storyEditText != null) {
                    storyEditText.setVisibility(View.VISIBLE);
                    storyEditText.setText(currentStoryContent);
                    storyEditText.setEnabled(true); // 确保编辑框可以编辑
                    storyEditText.setFocusable(true);
                    storyEditText.setFocusableInTouchMode(true);

                    // 如果是用户需要自行编辑的模版，设置焦点
                    if (currentStoryContent.contains("请在此编辑您的故事")) {
                        storyEditText.requestFocus();
                    }
                }
                if (saveStoryButton != null) {
                    saveStoryButton.setVisibility(View.VISIBLE);
                }
            } catch (Exception e) {
                Log.e(TAG, "updateStory: 更新故事内容出错", e);
                showError();
            }
        });
    }

    /**
     * 保存故事到数据库
     */
    private void saveStory() {
        Log.d(TAG, "saveStory: 开始保存故事");
        Context context = safeContext != null ? safeContext : getContext();
        if (context == null) {
            Log.e(TAG, "saveStory: Context is null");
            return;
        }

        try {
            // 检查故事编辑框是否存在
            if (storyEditText == null) {
                Log.e(TAG, "saveStory: storyEditText is null");
                Toast.makeText(context, "错误: 故事编辑框不存在", Toast.LENGTH_SHORT).show();
                return;
            }

            String storyContent = storyEditText.getText().toString().trim();
            Log.d(TAG, "saveStory: 获取到故事内容, 长度: " + storyContent.length());

            // 检查内容
            if (TextUtils.isEmpty(storyContent) || storyContent.equals("请在此编辑您的故事...")) {
                Log.w(TAG, "saveStory: 故事内容为空");
                Toast.makeText(context, R.string.story_empty_error, Toast.LENGTH_SHORT).show();
                return;
            }

            // 检查歌曲URI
            if (TextUtils.isEmpty(currentSongUri)) {
                Log.e(TAG, "saveStory: 歌曲URI为空");
                Toast.makeText(context, R.string.error_saving_story, Toast.LENGTH_SHORT).show();
                return;
            }

            // 显示保存中提示
            Toast.makeText(context, "正在保存故事...", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "saveStory: 准备保存故事. URI: " + currentSongUri + ", 内容长度: " + storyContent.length());

            // 禁用按钮防止重复点击
            if (saveStoryButton != null) {
                saveStoryButton.setEnabled(false);
            }

            // 更新当前故事内容
            currentStoryContent = storyContent;

            // 在后台线程中保存故事
            new Thread(() -> {
                try {
                    Log.d(TAG, "saveStory: 后台线程启动");
                    AppDatabase database = AppDatabase.getInstance(context);

                    if (database == null) {
                        Log.e(TAG, "saveStory: 获取数据库实例失败");
                        showToast("数据库初始化失败");
                        return;
                    }

                    StoryEntity story = new StoryEntity(currentSongUri, currentKeywords, storyContent);
                    Log.d(TAG, "saveStory: 创建故事实体对象");

                    long id = database.storyDao().insert(story);
                    Log.d(TAG, "saveStory: 故事已保存, ID: " + id);

                    if (id > 0) {
                        showToast(context.getString(R.string.story_saved));
                        Log.d(TAG, "saveStory: 显示成功提示");
                    } else {
                        Log.e(TAG, "saveStory: 插入返回无效ID: " + id);
                        showToast(context.getString(R.string.error_saving_story));
                    }

                    // 重新启用按钮
                    runOnUiThread(() -> {
                        if (saveStoryButton != null) {
                            saveStoryButton.setEnabled(true);
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "saveStory: 保存时异常", e);
                    showToast("保存故事时出错: " + e.getMessage());
                    runOnUiThread(() -> {
                        if (saveStoryButton != null) {
                            saveStoryButton.setEnabled(true);
                        }
                    });
                }
            }).start();
        } catch (Exception e) {
            Log.e(TAG, "saveStory: 意外异常", e);
            Toast.makeText(context, "意外错误: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            if (saveStoryButton != null) {
                saveStoryButton.setEnabled(true);
            }
        }
    }

    // 安全地在UI线程上运行代码
    private void runOnUiThread(Runnable action) {
        try {
            if (getActivity() != null && isAdded()) {
                getActivity().runOnUiThread(action);
            } else {
                // 如果Fragment未附加到Activity，使用Handler在主线程上运行
                mainHandler.post(action);
            }
        } catch (Exception e) {
            Log.e(TAG, "runOnUiThread: 运行UI线程代码出错", e);
        }
    }

    // 安全地显示Toast
    private void showToast(final String message) {
        runOnUiThread(() -> {
            try {
                Context context = safeContext != null ? safeContext : getContext();
                if (context != null) {
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Log.e(TAG, "showToast: 显示Toast出错", e);
            }
        });
    }

    @Override
    public void onDetach() {
        super.onDetach();
        // 保持safeContext引用以备后用
        Log.d(TAG, "onDetach: 片段已分离");
    }
}