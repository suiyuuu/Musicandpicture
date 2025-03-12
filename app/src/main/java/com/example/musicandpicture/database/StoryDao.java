package com.example.musicandpicture.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface StoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(StoryEntity story);

    @Update
    void update(StoryEntity story);

    @Query("SELECT * FROM stories WHERE id = :id")
    StoryEntity getStoryById(int id);

    @Query("SELECT * FROM stories WHERE songUri = :songUri ORDER BY timestamp DESC LIMIT 1")
    StoryEntity getLatestStoryForSong(String songUri);

    @Query("SELECT * FROM stories ORDER BY timestamp DESC")
    List<StoryEntity> getAllStories();

    @Query("SELECT * FROM stories WHERE songUri = :songUri ORDER BY timestamp DESC")
    List<StoryEntity> getStoriesForSong(String songUri);
}