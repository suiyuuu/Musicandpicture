package com.example.musicandpicture.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ImageNoteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ImageNoteEntity imageNote);

    @Update
    void update(ImageNoteEntity imageNote);

    @Query("SELECT * FROM image_notes WHERE uri = :uri")
    ImageNoteEntity getImageNoteByUri(String uri);

    @Query("SELECT * FROM image_notes")
    List<ImageNoteEntity> getAllImageNotes();

    @Query("UPDATE image_notes SET notes = :notes WHERE uri = :uri")
    void updateNotes(String uri, String notes);

    @Query("UPDATE image_notes SET appliedFilter = :filter WHERE uri = :uri")
    void updateFilter(String uri, String filter);
}
