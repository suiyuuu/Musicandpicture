package com.example.musicandpicture.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {SongEntity.class, ImageNoteEntity.class, StoryEntity.class}, version = 1,exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static final String DATABASE_NAME = "musicandpicture.db";
    private static volatile AppDatabase instance;

    // DAO接口
    public abstract SongDao songDao();
    public abstract ImageNoteDao imageNoteDao();
    public abstract StoryDao storyDao();

    // 单例模式获取数据库实例
    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            DATABASE_NAME)
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }
}