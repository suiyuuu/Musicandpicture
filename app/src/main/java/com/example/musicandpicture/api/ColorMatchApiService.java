package com.example.musicandpicture.api;

import android.util.Log;

import com.example.musicandpicture.MediaItem;
import com.example.musicandpicture.model.ColorMatchRequest;
import com.example.musicandpicture.model.ColorMatchResponse;
import com.example.musicandpicture.model.ColorVector;
import com.example.musicandpicture.model.MatchResult;
import com.example.musicandpicture.model.MusicItemDto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.POST;

/**
 * 颜色匹配API服务
 */
public class ColorMatchApiService {
    private static final String TAG = "ColorMatchApiService";
    private static final String BASE_URL = "http://192.168.10.186:5000/"; // 本地开发服务器地址
    private static final boolean USE_LOCAL_FALLBACK = true; // 使用本地回退算法

    private final ColorMatchApi api;

    /**
     * ColorMatch API接口
     */
    public interface ColorMatchApi {
        @POST("match")
        Call<ColorMatchResponse> matchMusic(@Body ColorMatchRequest request);
    }

    /**
     * ColorMatch API回调接口
     */
    public interface ColorMatchCallback {
        void onSuccess(List<MatchResult> results);
        void onFailure(String errorMessage);
    }

    public ColorMatchApiService() {
        // 创建日志拦截器
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        // 创建OkHttpClient并配置超时
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .addInterceptor(loggingInterceptor)
                .build();

        // 创建Retrofit实例
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        // 创建API服务
        api = retrofit.create(ColorMatchApi.class);
    }

    /**
     * 发送颜色匹配请求
     */
    public void matchMusic(float[] colorVector, List<MediaItem> musicItems, ColorMatchCallback callback) {
        // 创建请求对象
        ColorMatchRequest request = createRequest(colorVector, musicItems);

        // 如果使用本地回退，直接在本地计算并返回
        if (USE_LOCAL_FALLBACK) {
            List<MatchResult> results = calculateMatchLocally(colorVector, musicItems);
            callback.onSuccess(results);
            return;
        }

        // 发送网络请求
        api.matchMusic(request).enqueue(new Callback<ColorMatchResponse>() {
            @Override
            public void onResponse(Call<ColorMatchResponse> call, Response<ColorMatchResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "API response successful");
                    callback.onSuccess(response.body().getMatchedItems());
                } else {
                    Log.e(TAG, "API error: " + response.code());

                    // 尝试使用本地计算
                    if (USE_LOCAL_FALLBACK) {
                        List<MatchResult> results = calculateMatchLocally(colorVector, musicItems);
                        callback.onSuccess(results);
                    } else {
                        callback.onFailure("API返回错误: " + response.code());
                    }
                }
            }

            @Override
            public void onFailure(Call<ColorMatchResponse> call, Throwable t) {
                Log.e(TAG, "API request failed", t);

                // 尝试使用本地计算
                if (USE_LOCAL_FALLBACK) {
                    List<MatchResult> results = calculateMatchLocally(colorVector, musicItems);
                    callback.onSuccess(results);
                } else {
                    callback.onFailure("网络请求失败: " + t.getMessage());
                }
            }
        });
    }

    /**
     * 在本地计算匹配结果，用于服务不可用时的回退
     */
    private List<MatchResult> calculateMatchLocally(float[] colorVector, List<MediaItem> musicItems) {
        List<MatchResult> results = new ArrayList<>();

        // 对每首音乐计算匹配度
        for (MediaItem musicItem : musicItems) {
            String[] keywords = getKeywords(musicItem);
            float matchScore = calculateMatchScore(keywords, colorVector);

            // 创建匹配结果对象
            MatchResult result = new MatchResult();
            result.setMusicId(musicItem.getUri().toString());
            result.setMatchScore(matchScore);

            results.add(result);
        }

        // 按匹配度排序
        results.sort((a, b) -> Float.compare(b.getMatchScore(), a.getMatchScore()));

        return results;
    }

    /**
     * 创建API请求对象
     */
    private ColorMatchRequest createRequest(float[] colorVector, List<MediaItem> musicItems) {
        ColorMatchRequest request = new ColorMatchRequest();

        // 设置颜色向量
        ColorVector cv = new ColorVector();
        cv.setValues(colorVector);
        request.setColorVector(cv);

        // 设置音乐项
        List<MusicItemDto> musicItemDtos = new ArrayList<>();
        for (MediaItem musicItem : musicItems) {
            MusicItemDto dto = new MusicItemDto();
            dto.setId(musicItem.getUri().toString());
            dto.setName(musicItem.getSongName() != null ?
                    musicItem.getSongName() : musicItem.getFileName());
            dto.setArtist(musicItem.getArtistName());

            // 获取关键词
            String[] keywords = getKeywords(musicItem);
            dto.setKeywords(keywords);

            musicItemDtos.add(dto);
        }
        request.setMusicItems(musicItemDtos);

        return request;
    }

    /**
     * 获取音乐项的关键词
     */
    private String[] getKeywords(MediaItem musicItem) {
        // 若没有关键词，生成随机关键词
        if (musicItem.getKeywords() == null || musicItem.getKeywords().trim().isEmpty()) {
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
        KeywordColorMap keywordColorMap = new KeywordColorMap();

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
     * 关键词到颜色的映射表
     */
    private static class KeywordColorMap {
        private final Map<String, float[]> map = new HashMap<>();

        public KeywordColorMap() {
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
        }

        public boolean containsKey(String key) {
            return map.containsKey(key);
        }

        public float[] get(String key) {
            return map.get(key);
        }
    }
}