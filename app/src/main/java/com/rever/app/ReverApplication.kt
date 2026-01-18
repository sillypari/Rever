package com.rever.app

import android.app.Application
import com.rever.app.data.database.ReverDatabase
import com.rever.app.data.preferences.UserPreferencesRepository
import com.rever.app.data.repository.RevisionSessionRepository
import com.rever.app.data.repository.SubjectRepository
import com.rever.app.data.repository.TopicRepository

class ReverApplication : Application() {
    
    val database by lazy { ReverDatabase.getDatabase(this) }
    
    val subjectRepository by lazy { SubjectRepository(database.subjectDao()) }
    val topicRepository by lazy { TopicRepository(database.topicDao()) }
    val revisionSessionRepository by lazy { RevisionSessionRepository(database.revisionSessionDao()) }
    val userPreferencesRepository by lazy { UserPreferencesRepository(this) }
}
