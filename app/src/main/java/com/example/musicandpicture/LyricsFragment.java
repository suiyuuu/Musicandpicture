package com.example.musicandpicture;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.musicandpicture.R;

public class LyricsFragment extends Fragment {

    private TextView lyricsTextView;
    private ProgressBar lyricsProgressBar;
    private TextView lyricsErrorTextView;

    public LyricsFragment() {
        // 必需的空构造函数
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_lyrics, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 初始化视图
        lyricsTextView = view.findViewById(R.id.lyricsTextView);
        lyricsProgressBar = view.findViewById(R.id.lyricsProgressBar);
        lyricsErrorTextView = view.findViewById(R.id.lyricsErrorTextView);

        // 初始状态为加载中
        showLoading();
    }

    /**
     * 显示加载中状态
     */
    public void showLoading() {
        if (lyricsProgressBar != null) {
            lyricsProgressBar.setVisibility(View.VISIBLE);
        }
        if (lyricsTextView != null) {
            lyricsTextView.setVisibility(View.GONE);
        }
        if (lyricsErrorTextView != null) {
            lyricsErrorTextView.setVisibility(View.GONE);
        }
    }

    /**
     * 显示错误状态
     */
    public void showError() {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (lyricsProgressBar != null) {
                    lyricsProgressBar.setVisibility(View.GONE);
                }
                if (lyricsTextView != null) {
                    lyricsTextView.setVisibility(View.GONE);
                }
                if (lyricsErrorTextView != null) {
                    lyricsErrorTextView.setVisibility(View.VISIBLE);
                }
            });
        }
    }

    /**
     * 更新歌词内容
     * @param lyrics 歌词文本
     */
    public void updateLyrics(String lyrics) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (lyricsProgressBar != null) {
                    lyricsProgressBar.setVisibility(View.GONE);
                }
                if (lyricsErrorTextView != null) {
                    lyricsErrorTextView.setVisibility(View.GONE);
                }
                if (lyricsTextView != null) {
                    lyricsTextView.setVisibility(View.VISIBLE);
                    lyricsTextView.setText(lyrics);
                }
            });
        }
    }
}