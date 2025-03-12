// ApiService.java
package com.example.musicandpicture.api;

import android.util.Log; // 添加日志导入

import com.example.musicandpicture.model.SongResponse;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.POST;

import org.json.JSONException;
import org.json.JSONObject;

public class ApiService {
    private static final String TAG = "ApiService"; // 添加TAG常量
    private static final String BASE_URL = "http://192.168.26.250:5000/"; // 使用ADB端口转发，别再给我报错了，求苏骥辉包邮

    private SongApi songApi;

    public ApiService() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
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
                        if (response.body() != null) {
                            Log.d(TAG, "Response body: " + response.body().toString());
                        }
                    } else {
                        Log.e(TAG, "Response error: " + response.code());
                        try {
                            Log.e(TAG, "Error body: " + response.errorBody().string());
                        } catch (Exception e) {
                            Log.e(TAG, "Error reading error body", e);
                        }
                    }
                    callback.onResponse(call, response);
                }

                @Override
                public void onFailure(Call<SongResponse> call, Throwable t) {
                    // 记录失败日志
                    Log.e(TAG, "Request failed", t);
                    callback.onFailure(call, t);
                }
            });

        } catch (JSONException e) {
            Log.e(TAG, "JSON creation error", e);
        }
    }
}