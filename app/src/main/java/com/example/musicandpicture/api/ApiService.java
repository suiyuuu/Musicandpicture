// ApiService.java
package com.example.musicandpicture.api;

import android.util.Log;

import com.example.musicandpicture.model.SongResponse;
import com.example.musicandpicture.util.KeywordExtractor;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.POST;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ApiService {
    private static final String TAG = "ApiService";

    // 使用可配置的服务器地址，便于不同环境切换
    private static final String BASE_URL = "http://192.168.10.186:5000/";

    // 后备功能开关 - 当API服务不可用时启用本地处理
    private static final boolean ENABLE_FALLBACK = true;

    private SongApi songApi;
    private OkHttpClient okHttpClient;

    public ApiService() {
        // 创建日志拦截器
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        // 创建OkHttpClient并配置超时
        okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .addInterceptor(loggingInterceptor)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        songApi = retrofit.create(SongApi.class);
    }

    public interface SongApi {
        @POST("api/process-song")
        Call<SongResponse> processSong(@Body RequestBody body);
    }

    public void processSong(String fileName, final Callback<SongResponse> callback) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("file_name", fileName);

            // 添加详细日志
            Log.d(TAG, "Preparing to send song: " + fileName);
            Log.d(TAG, "JSON Payload: " + jsonObject.toString());

            RequestBody body = RequestBody.create(
                    MediaType.parse("application/json"),
                    jsonObject.toString()
            );

            Call<SongResponse> call = songApi.processSong(body);
            call.enqueue(new Callback<SongResponse>() {
                @Override
                public void onResponse(Call<SongResponse> call, Response<SongResponse> response) {
                    // 记录响应日志
                    Log.d(TAG, "Response received");
                    if (response.isSuccessful()) {
                        Log.d(TAG, "Response successful");
                        SongResponse songResponse = response.body();

                        if (songResponse != null) {
                            Log.d(TAG, "Response body: " + songResponse.toString());

                            // 检查关键词是否为空，如果为空，尝试本地提取
                            if ((songResponse.getKeywords() == null || songResponse.getKeywords().isEmpty()) &&
                                    songResponse.getLyrics() != null && !songResponse.getLyrics().isEmpty() &&
                                    ENABLE_FALLBACK) {

                                Log.d(TAG, "No keywords from server, doing local extraction");
                                String lyrics = songResponse.getLyrics();
                                String[] keywords = KeywordExtractor.extractKeywordsFromLyrics(lyrics, 10);

                                if (keywords.length > 0) {
                                    List<String> keywordsList = Arrays.asList(keywords);
                                    Log.d(TAG, "Locally extracted keywords: " + keywordsList);
                                    songResponse.setKeywords(new ArrayList<>(keywordsList));
                                }
                            }
                        }

                        callback.onResponse(call, response);
                    } else {
                        Log.e(TAG, "Response error: " + response.code());
                        try {
                            Log.e(TAG, "Error body: " + response.errorBody().string());

                            // 使用本地后备方案
                            if (ENABLE_FALLBACK) {
                                handleFallback(fileName, callback);
                            } else {
                                callback.onResponse(call, response);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error reading error body", e);
                            callback.onResponse(call, response);
                        }
                    }
                }

                @Override
                public void onFailure(Call<SongResponse> call, Throwable t) {
                    // 记录失败日志
                    Log.e(TAG, "Request failed", t);

                    // 使用本地后备方案
                    if (ENABLE_FALLBACK) {
                        handleFallback(fileName, callback);
                    } else {
                        callback.onFailure(call, t);
                    }
                }
            });

        } catch (JSONException e) {
            Log.e(TAG, "JSON creation error", e);

            // 使用本地后备方案
            if (ENABLE_FALLBACK) {
                handleFallback(fileName, callback);
            } else {
                callback.onFailure(null, e);
            }
        }
    }

    /**
     * 本地后备处理方案 - 当API服务不可用时使用
     */
    private void handleFallback(String fileName, Callback<SongResponse> callback) {
        Log.d(TAG, "Using fallback for: " + fileName);

        // 创建一个基本响应
        SongResponse fallbackResponse = new SongResponse();

        // 设置基本信息 - 从文件名推断
        String[] parts = fileName.split("\\.");
        String nameWithoutExt = parts[0];

        if (nameWithoutExt.contains(" - ")) {
            String[] nameParts = nameWithoutExt.split(" - ", 2);
            fallbackResponse.setArtistName(nameParts[0].trim());
            fallbackResponse.setSongName(nameParts[1].trim());
        } else {
            fallbackResponse.setSongName(nameWithoutExt);
            fallbackResponse.setArtistName("未知艺术家");
        }

        // 设置默认歌词
        fallbackResponse.setLyrics("无法从服务器获取歌词。\n你可以在这里欣赏音乐，或者尝试其他歌曲。");

        // 生成一些默认关键词
        List<String> defaultKeywords = new ArrayList<>();
        defaultKeywords.add("音乐");
        defaultKeywords.add("旋律");
        defaultKeywords.add("节奏");
        defaultKeywords.add("歌曲");
        defaultKeywords.add("聆听");
        defaultKeywords.add("情感");

        if (fallbackResponse.getArtistName() != null && !fallbackResponse.getArtistName().equals("未知艺术家")) {
            defaultKeywords.add(fallbackResponse.getArtistName());
        }

        if (nameWithoutExt.toLowerCase().contains("爱")) defaultKeywords.add("爱");
        if (nameWithoutExt.toLowerCase().contains("快乐")) defaultKeywords.add("快乐");
        if (nameWithoutExt.toLowerCase().contains("悲")) defaultKeywords.add("悲伤");
        if (nameWithoutExt.toLowerCase().contains("怀念")) defaultKeywords.add("回忆");
        if (nameWithoutExt.toLowerCase().contains("梦")) defaultKeywords.add("梦想");

        fallbackResponse.setKeywords(defaultKeywords);

        // 生成默认故事
        String defaultStory = "这是一首名为《" + fallbackResponse.getSongName() + "》的歌曲";
        if (!fallbackResponse.getArtistName().equals("未知艺术家")) {
            defaultStory += "，由" + fallbackResponse.getArtistName() + "演唱";
        }
        defaultStory += "。\n\n无法从服务器获取故事内容，但您可以根据这首歌曲的感受，在此创作自己的故事...";

        fallbackResponse.setStory(defaultStory);

        // 创建成功响应
        Response<SongResponse> successResponse = Response.success(fallbackResponse);
        callback.onResponse(null, successResponse);
    }
}