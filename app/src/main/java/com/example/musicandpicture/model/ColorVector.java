package com.example.musicandpicture.model;

import com.google.gson.annotations.SerializedName;

/**
 * 颜色向量
 */
public class ColorVector {
    @SerializedName("values")
    private float[] values;

    public float[] getValues() {
        return values;
    }

    public void setValues(float[] values) {
        this.values = values;
    }
}