package com.example.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.example.core.model.ChatMessage
import com.example.core.model.IntentType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long,
    val intentTypeName: String? = null,
    val statusSuccess: Boolean? = null,
    val searchQuery: String? = null
) {
    fun toChatMessage(): ChatMessage = ChatMessage(
        id = id,
        text = text,
        isUser = isUser,
        timestamp = timestamp,
        intentType = intentTypeName?.let { runCatching { IntentType.valueOf(it) }.getOrNull() },
        statusSuccess = statusSuccess,
        searchQuery = searchQuery
    )
}

@Entity(tableName = "command_audit_logs")
data class CommandAuditEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rawPrompt: String,
    val intentTypeName: String,
    val executionSuccess: Boolean,
    val responseSummary: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentMessages(limit: Int = 50): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM (SELECT * FROM chat_messages ORDER BY timestamp DESC LIMIT :limit) ORDER BY timestamp ASC")
    suspend fun getRecentMessagesList(limit: Int = 10): List<ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Query("DELETE FROM chat_messages")
    suspend fun clearAllMessages()
}

@Dao
interface CommandAuditDao {
    @Query("SELECT * FROM command_audit_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogs(limit: Int = 100): Flow<List<CommandAuditEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: CommandAuditEntity): Long

    @Query("DELETE FROM command_audit_logs")
    suspend fun clearAuditLogs()
}

@Database(
    entities = [ChatMessageEntity::class, CommandAuditEntity::class],
    version = 2,
    exportSchema = false
)
abstract class PeterDatabase : RoomDatabase() {
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun commandAuditDao(): CommandAuditDao

    companion object {
        @Volatile
        private var INSTANCE: PeterDatabase? = null

        fun getDatabase(context: Context): PeterDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PeterDatabase::class.java,
                    "peter_assistant.db"
                ).fallbackToDestructiveMigration(dropAllTables = true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
