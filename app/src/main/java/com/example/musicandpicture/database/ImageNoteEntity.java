package com.example.musicandpicture.database;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "image_notes")
public class ImageNoteEntity {

    @PrimaryKey
    @NonNull
    private String uri;

    private String notes;
    private String appliedFilter;

    public ImageNoteEntity(@NonNull String uri) {
        this.uri = uri;
    }

    @NonNull
    public String getUri() {
        return uri;
    }

    public void setUri(@NonNull String uri) {
        this.uri = uri;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getAppliedFilter() {
        return appliedFilter;
    }

    public void setAppliedFilter(String appliedFilter) {
        this.appliedFilter = appliedFilter;
    }
}
