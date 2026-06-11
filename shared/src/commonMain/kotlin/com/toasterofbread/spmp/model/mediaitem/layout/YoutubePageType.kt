package com.toasterofbread.spmp.model.mediaitem.layout

import com.toasterofbread.spmp.model.mediaitem.enums.MediaItemType
import com.toasterofbread.spmp.model.mediaitem.getMediaItemFromUid
import com.toasterofbread.spmp.model.mediaitem.getUid
import com.toasterofbread.spmp.model.mediaitem.toMediaItemRef
import com.toasterofbread.spmp.platform.getUiLanguage
import com.toasterofbread.spmp.service.playercontroller.PlayerState
import dev.toastbits.ytmkt.model.external.CustomYoutubePage
import dev.toastbits.ytmkt.model.external.ListPageBrowseIdYoutubePage
import dev.toastbits.ytmkt.model.external.MediaItemYoutubePage
import dev.toastbits.ytmkt.model.external.PlainYoutubePage
import dev.toastbits.ytmkt.model.external.YoutubePage
import dev.toastbits.ytmkt.uistrings.UiString
import kotlinx.coroutines.runBlocking

enum class YoutubePageType {
    MediaItem, ListPage, Plain;

    fun getPage(data: String): YoutubePage {
        try {
            when (this) {
                MediaItem -> {
                    val split: List<String?> = data.split(VIEW_MORE_SPLIT_CHAR, limit = 3).map { it.ifBlank { null } }
                    return MediaItemYoutubePage(
                        browseMediaItem = getMediaItemFromUid(split[0]!!),
                        browseParams = split.getOrNull(1),
                        mediaItem = split.getOrNull(2)?.let { getMediaItemFromUid(it) }
                    )
                }
                ListPage -> {
                    val split: List<String> = data.split(VIEW_MORE_SPLIT_CHAR, limit = 3)
                    return ListPageBrowseIdYoutubePage(
                        mediaItem = getMediaItemFromUid(split[0], MediaItemType.ARTIST),
                        listPageBrowseId = split[1],
                        browseParams = split[2]
                    )
                }
                Plain -> {
                    return PlainYoutubePage(browseId = data)
                }
            }
        }
        catch (e: Throwable) {
            throw RuntimeException("Parsing ViewMore($this) data failed '$data'", e)
        }
    }

    companion object {
        private const val VIEW_MORE_SPLIT_CHAR = '|'

        fun fromPage(view_more: YoutubePage): Pair<Long, String>? =
            when (view_more) {
                is MediaItemYoutubePage ->
                    Pair(
                        MediaItem.ordinal.toLong(),
                        view_more.browseMediaItem.getUid() + VIEW_MORE_SPLIT_CHAR + (view_more.browseParams ?: "") + VIEW_MORE_SPLIT_CHAR + (view_more.mediaItem?.getUid() ?: "")
                    )
                is ListPageBrowseIdYoutubePage ->
                    Pair(
                        ListPage.ordinal.toLong(),
                        view_more.mediaItem.getUid() + VIEW_MORE_SPLIT_CHAR + view_more.listPageBrowseId + VIEW_MORE_SPLIT_CHAR + view_more.browseParams
                    )
                is PlainYoutubePage ->
                    Pair(
                        Plain.ordinal.toLong(),
                        view_more.browseId
                    )
                is LambdaYoutubePage -> null
                else -> throw NotImplementedError(view_more::class.toString())
            }
    }
}

data class LambdaYoutubePage(
    val action: (player: PlayerState, title: UiString?) -> Unit
): CustomYoutubePage {
    override fun getBrowseParamsData(): YoutubePage.BrowseParamsData =
        throw IllegalStateException()
}

suspend fun YoutubePage.open(player: PlayerState, title: UiString?) {
    when (this) {
        is LambdaYoutubePage ->  action(player, title)
        is MediaItemYoutubePage ->
            player.openMediaItem(
                (mediaItem ?: browseMediaItem).toMediaItemRef(),
                true,
                browse_params = browseParams?.let {
                    YoutubePage.BrowseParamsData(browseMediaItem.id, it)
                }
            )
        is ListPageBrowseIdYoutubePage ->
            player.openMediaItem(
                mediaItem.toMediaItemRef(),
                browse_params = getBrowseParamsData()
            )
        is PlainYoutubePage ->
            player.openViewMorePage(browseId, runBlocking { title?.getString(player.context.getUiLanguage().toTag()) })

        is CustomYoutubePage -> throw UnsupportedOperationException("Unknown CustomYoutubePage $this (${this::class})")
    }
}
