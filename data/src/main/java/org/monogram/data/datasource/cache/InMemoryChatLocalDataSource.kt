package org.monogram.data.datasource.cache

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.monogram.data.db.model.ChatEntity
import org.monogram.data.db.model.MessageEntity

class InMemoryChatLocalDataSource : ChatLocalDataSource {
    override fun getAllChats(): Flow<List<ChatEntity>> = flowOf(emptyList())

    override suspend fun insertChat(chat: ChatEntity) {}

    override suspend fun insertChats(chats: List<ChatEntity>) {}

    override suspend fun deleteChat(chatId: Long) {}

    override suspend fun clearAllChats() {}

    override fun getMessagesForChat(chatId: Long): Flow<List<MessageEntity>> = flowOf(emptyList())

    override suspend fun getMessagesOlder(chatId: Long, fromMessageId: Long, limit: Int): List<MessageEntity> =
        emptyList()

    override suspend fun getMessagesNewer(chatId: Long, fromMessageId: Long, limit: Int): List<MessageEntity> =
        emptyList()

    override suspend fun insertMessage(message: MessageEntity) {}

    override suspend fun insertMessages(messages: List<MessageEntity>) {}

    override suspend fun deleteMessage(messageId: Long) {}

    override suspend fun clearMessagesForChat(chatId: Long) {}

    override suspend fun deleteExpired(timestamp: Long) {}
}