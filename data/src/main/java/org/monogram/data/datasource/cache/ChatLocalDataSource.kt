package org.monogram.data.datasource.cache

import kotlinx.coroutines.flow.Flow
import org.monogram.data.db.model.ChatEntity
import org.monogram.data.db.model.MessageEntity

interface ChatLocalDataSource {
    fun getAllChats(): Flow<List<ChatEntity>>
    suspend fun insertChat(chat: ChatEntity)
    suspend fun insertChats(chats: List<ChatEntity>)
    suspend fun deleteChat(chatId: Long)
    suspend fun clearAllChats()

    fun getMessagesForChat(chatId: Long): Flow<List<MessageEntity>>
    suspend fun getMessagesOlder(chatId: Long, fromMessageId: Long, limit: Int): List<MessageEntity>
    suspend fun getMessagesNewer(chatId: Long, fromMessageId: Long, limit: Int): List<MessageEntity>
    suspend fun insertMessage(message: MessageEntity)
    suspend fun insertMessages(messages: List<MessageEntity>)
    suspend fun deleteMessage(messageId: Long)
    suspend fun clearMessagesForChat(chatId: Long)

    suspend fun deleteExpired(timestamp: Long)
}