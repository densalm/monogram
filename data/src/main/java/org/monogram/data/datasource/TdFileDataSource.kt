package org.monogram.data.datasource

import kotlinx.coroutines.CompletableDeferred
import org.drinkless.tdlib.TdApi
import org.monogram.data.core.coRunCatching
import org.monogram.data.gateway.TelegramGateway
import org.monogram.data.infra.FileDownloadQueue

class TdFileDataSource(
    private val gateway: TelegramGateway,
    private val fileDownloadQueue: FileDownloadQueue
) : FileDataSource {
    override suspend fun downloadFile(fileId: Int, priority: Int, offset: Long, limit: Long, synchronous: Boolean): TdApi.File?  {
        fileDownloadQueue.clearSuppression(fileId)
        fileDownloadQueue.enqueue(
            fileId,
            priority,
            FileDownloadQueue.DownloadType.DEFAULT,
            offset,
            limit,
            synchronous,
            ignoreSuppression = true
        )
        if (synchronous) {
            coRunCatching { fileDownloadQueue.waitForDownload(fileId).await() }
        }
        return getFile(fileId)
    }

    override suspend fun cancelDownload(fileId: Int): TdApi.Ok? {
        fileDownloadQueue.cancelDownload(fileId, force = true)
        val result = gateway.execute(TdApi.CancelDownloadFile(fileId, false))
        return if (result is TdApi.Ok) result else null
    }

    override suspend fun getFile(fileId: Int): TdApi.File? {
        return coRunCatching { gateway.execute(TdApi.GetFile(fileId)) }.getOrNull()
    }

    override suspend fun getFileDownloadedPrefixSize(fileId: Int, offset: Long): TdApi.FileDownloadedPrefixSize? {
        return coRunCatching { gateway.execute(TdApi.GetFileDownloadedPrefixSize(fileId, offset)) }.getOrNull()
    }

    override suspend fun readFilePart(fileId: Int, offset: Long, count: Long): TdApi.Data? {
        return coRunCatching { gateway.execute(TdApi.ReadFilePart(fileId, offset, count)) }.getOrNull()
    }

    override fun waitForUpload(fileId: Int): CompletableDeferred<Unit> {
        return fileDownloadQueue.waitForUpload(fileId)
    }
}