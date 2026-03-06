package org.monogram.data.datasource.cache

import org.drinkless.tdlib.TdApi

interface UserLocalDataSource {
    suspend fun getUser(userId: Long): TdApi.User?
    suspend fun putUser(user: TdApi.User)
    suspend fun getUserFullInfo(userId: Long): TdApi.UserFullInfo?
    suspend fun putUserFullInfo(userId: Long, info: TdApi.UserFullInfo)
    suspend fun getAllUsers(): Collection<TdApi.User>
    suspend fun clearAll()
}