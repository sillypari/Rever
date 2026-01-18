package com.rever.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.rever.app.data.dao.RevisionSessionDao
import com.rever.app.data.dao.SubjectDao
import com.rever.app.data.dao.TopicDao
import com.rever.app.data.model.RevisionSession
import com.rever.app.data.model.Subject
import com.rever.app.data.model.Topic

@Database(
    entities = [
        Subject::class,
        Topic::class,
        RevisionSession::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class ReverDatabase : RoomDatabase() {
    
    abstract fun subjectDao(): SubjectDao
    abstract fun topicDao(): TopicDao
    abstract fun revisionSessionDao(): RevisionSessionDao
    
    companion object {
        @Volatile
        private var INSTANCE: ReverDatabase? = null
        
        fun getDatabase(context: Context): ReverDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ReverDatabase::class.java,
                    "rever_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
