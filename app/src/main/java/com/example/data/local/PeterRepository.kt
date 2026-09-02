package com.example.data.local

import com.example.core.model.ChatMessage
import com.example.core.model.CommandResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PeterRepository(
    private val database: PeterDatabase,
    val preferences: PeterPreferences
) {
    private val chatMessageDao = database.chatMessageDao()
    private val commandAuditDao = database.commandAuditDao()

    val messages: Flow<List<ChatMessage>> = chatMessageDao.getAllMessages().map { entities ->
        entities.map { it.toChatMessage() }
    }

    suspend fun getRecentMessages(limit: Int = 10): List<ChatMessage> {
        return chatMessageDao.getRecentMessagesList(limit).map { it.toChatMessage() }
    }

    suspend fun saveMessage(message: ChatMessage): Long {
        return chatMessageDao.insertMessage(
            ChatMessageEntity(
                text = message.text,
                isUser = message.isUser,
                timestamp = message.timestamp,
                intentTypeName = message.intentType?.name,
                statusSuccess = message.statusSuccess,
                searchQuery = message.searchQuery
            )
        )
    }

    suspend fun recordAuditLog(rawPrompt: String, commandResult: CommandResult) {
        commandAuditDao.insertLog(
            CommandAuditEntity(
                rawPrompt = rawPrompt,
                intentTypeName = commandResult.intentType.name,
                executionSuccess = commandResult.success,
                responseSummary = commandResult.spokenResponse,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun clearHistory() {
        chatMessageDao.clearAllMessages()
        commandAuditDao.clearAuditLogs()
    }
}
