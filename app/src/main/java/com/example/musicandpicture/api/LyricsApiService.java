package com.example.musicandpicture.api;

import com.example.musicandpicture.model.LyricsResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface LyricsApiService {

    // 这里使用的是模拟API接口，实际使用时需要替换成真实的歌词API
    @GET("search")
    Call<LyricsResponse> searchLyrics(
            @Query("artist") String artist,
            @Query("song") String song
    );
}