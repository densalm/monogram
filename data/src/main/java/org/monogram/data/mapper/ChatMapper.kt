package org.monogram.data.mapper

import org.monogram.data.db.model.ChatEntity
import org.monogram.domain.models.ChatModel

class ChatMapper {
    fun mapToDomain(entity: ChatEntity): ChatModel {
        return ChatModel(
            id = entity.id,
            title = entity.title,
            unreadCount = entity.unreadCount,
            avatarPath = entity.avatarPath,
            lastMessageText = entity.lastMessageText,
            lastMessageTime = entity.lastMessageTime,
            order = entity.order,
            isPinned = entity.isPinned
        )
    }

    fun mapToEntity(domain: ChatModel): ChatEntity {
        return ChatEntity(
            id = domain.id,
            title = domain.title,
            unreadCount = domain.unreadCount,
            avatarPath = domain.avatarPath,
            lastMessageText = domain.lastMessageText,
            lastMessageTime = domain.lastMessageTime,
            order = domain.order,
            isPinned = domain.isPinned,
            createdAt = System.currentTimeMillis()
        )
    }
}