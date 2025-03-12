package com.example.musicandpicture.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface SongDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(SongEntity song);

    @Update
    void update(SongEntity song);

    @Query("SELECT * FROM songs WHERE uri = :uri")
    SongEntity getSongByUri(String uri);

    @Query("SELECT * FROM songs WHERE songName LIKE '%' || :query || '%' OR artistName LIKE '%' || :query || '%'")
    List<SongEntity> searchSongs(String query);

    @Query("SELECT * FROM songs")
    List<SongEntity> getAllSongs();

    @Query("UPDATE songs SET lyrics = :lyrics WHERE uri = :uri")
    void updateLyrics(String uri, String lyrics);

    @Query("UPDATE songs SET keywords = :keywords WHERE uri = :uri")
    void updateKeywords(String uri, String keywords);
}