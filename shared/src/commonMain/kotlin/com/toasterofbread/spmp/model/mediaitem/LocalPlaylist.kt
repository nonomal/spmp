package com.toasterofbread.spmp.model.mediaitem

import androidx.compose.runtime.Composable
import com.toasterofbread.Database
import com.toasterofbread.spmp.model.YoutubeMusicAuthInfo
import com.toasterofbread.spmp.model.mediaitem.enums.PlaylistType

fun Playlist.isLocalPlaylist(db: Database): Boolean {
    when (this) {
        is LocalPlaylistRef -> return true
        is AccountPlaylistRef -> return false
        is PlaylistData -> return TypeOfPlaylist.get(db) == PlaylistType.LOCAL
    }
}

fun Playlist.localPlaylistOrNull(db: Database): LocalPlaylistRef? =
    if (this is LocalPlaylistRef) this
    else if (!isLocalPlaylist(db)) null
    else LocalPlaylistRef(id)

@Composable
fun rememberLocalPlaylists(db: Database): List<LocalPlaylistRef> {
    return db.playlistQueries.byType(PlaylistType.LOCAL.ordinal.toLong())
        .observeAsState(
            {
                it.executeAsList().map { playlist_id ->
                    LocalPlaylistRef(playlist_id)
                }
            },
            null
        )
        .value
}

fun createLocalPlaylist(db: Database): LocalPlaylistRef {
    val local_type = PlaylistType.LOCAL.ordinal.toLong()

    val largest_local_id: Long = db.playlistQueries
        .getLargestIdByType(local_type)
        .executeAsOne().MAX?.toLongOrNull() ?: -1

    val playlist = LocalPlaylistRef((largest_local_id + 1).toString())
    db.playlistQueries.insertById(playlist.id, local_type)

    return playlist
}

fun Playlist.uploadLocalPlaylistToAccount(ytm_auth: YoutubeMusicAuthInfo) {
    require(ytm_auth.is_initialised)
}


//class LocalPlaylist(id: String, context: PlatformContext): Playlist(id, context) {
//    override val url: String? = checkNotDeleted(null)
//
//    override val data: PlaylistItemData = LocalPlaylistItemData(this)
//
//    override val items: List<MediaItem>? get() = checkNotDeleted(data.items!!)
//    override val is_editable: Boolean = checkNotDeleted(true)
//    override val playlist_type: PlaylistType = checkNotDeleted(TypeOfPlaylist.PLAYLIST)
//    override val total_duration: Long? get() {
//        checkNotDeleted()
//        var sum = 0L
//        for (item in items) {
//            if (item !is Song) {
//                continue
//            }
//            if (item.duration == null) {
//                return null
//            }
//            sum += item.duration!!
//        }
//        return sum
//    }
//    override val item_count: Int get() = checkNotDeleted(items.size)
//
//    override fun addItem(item: MediaItem) {
//        super.addItem(item)
//        data.onChanged()
//    }
//
//    override fun removeItem(index: Int) {
//        super.removeItem(index)
//        data.onChanged()
//    }
//
//    override fun moveItem(from: Int, to: Int) {
//        super.moveItem(from, to)
//        data.onChanged()
//    }
//
//    override suspend fun deletePlaylist(): Result<Unit> {
//        deleteLocalPlaylist(SpMp.context, id)
//        onDeleted()
//        return Result.success(Unit)
//    }
//
//    override suspend fun saveItems(): Result<Unit> {
//        checkNotDeleted()
//        data.save()
//        return Result.success(Unit)
//    }
//
//    override fun canLoadThumbnail(): Boolean = checkNotDeleted(true)
//    override fun downloadThumbnail(quality: MediaItemThumbnailProvider.Quality): Result<ImageBitmap> = checkNotDeleted(Result.failure(NotImplementedError()))
//
//    override fun canGetThemeColour(): Boolean = checkNotDeleted(true)
//    override fun getThemeColour(): Color = checkNotDeleted(super.getThemeColour() ?: Theme.accent)
//
//    @Composable
//    override fun Thumbnail(
//        quality: MediaItemThumbnailProvider.Quality,
//        modifier: Modifier,
//        failure_icon: ImageVector?,
//        contentColourProvider: (() -> Color)?,
//        onLoaded: ((ImageBitmap) -> Unit)?
//    ) {
//        checkNotDeleted()
//
//        var image_item: MediaItem? by remember { mutableStateOf(null) }
//        LaunchedEffect(playlist_reg_entry.image_item_uid) {
//            image_item = playlist_reg_entry.image_item_uid?.let { uid ->
//                fromUid(uid)
//            }
//        }
//
//        image_item?.also { item ->
//            item.Thumbnail(quality, modifier, failure_icon, contentColourProvider, onLoaded)
//            return
//        }
//
//        Box(modifier.background(Theme.accent), contentAlignment = Alignment.Center) {
//            Icon(
//                Icons.Default.MusicNote,
//                null,
//                tint = Theme.on_accent
//            )
//        }
//    }
//
//    suspend fun convertToAccountPlaylist(context: PlatformContext = SpMp.context): Result<PlaylistData> {
//        checkNotDeleted()
//        check(Api.ytm_authenticated)
//
//        val create_result = createAccountPlaylist(title.orEmpty(), description.orEmpty())
//        if (create_result.isFailure) {
//            return create_result.cast()
//        }
//
//        val playlist_id = create_result.getOrThrow().let {
//            if (!it.startsWith("VL")) "VL$it" else it
//        }
//
//        val add_result = addSongsToAccountPlaylist(playlist_id, items.mapNotNull { if (it is Song) it.id else null })
//        if (add_result.isFailure) {
//            return add_result.cast()
//        }
//
//        val account_playlist = PlaylistData(playlist_id)
//            .editPlaylistData {
//                supplyTitle(this@LocalPlaylist.title, true)
//                supplyDescription(this@LocalPlaylist.description, true)
//                supplyYear(this@LocalPlaylist.year, true)
//                supplyPlaylistType(TypeOfPlaylist.PLAYLIST, true)
//                supplyArtist(Api.ytm_auth.own_channel, true)
//                supplyItems(this@LocalPlaylist.items, true)
//            }
//
//        account_playlist.playlist_reg_entry.apply {
//            image_item_uid = playlist_reg_entry.image_item_uid
//            title = playlist_reg_entry.title
//            play_counts = playlist_reg_entry.play_counts
//            saveRegistry()
//        }
//
//        var load_result: Result<*> = account_playlist.getTitle()
//        if (load_result.isFailure) return load_result.cast()
//
//        load_result = account_playlist.getDescription()
//        if (load_result.isFailure) return load_result.cast()
//
//        load_result = account_playlist.getThumbnailProvider()
//        if (load_result.isFailure) return load_result.cast()
//
//        onReplaced(account_playlist)
//        deleteLocalPlaylist(context, id)
//
//        return Result.success(account_playlist)
//    }
//
//    override suspend fun loadGeneralData(item_id: String, browse_params: String?): Result<Unit> =
//        checkNotDeleted(Result.success(Unit))
//
//    interface Listener {
//        fun onAdded(playlist: LocalPlaylist)
//        fun onRemoved(index: Int, playlist: LocalPlaylist)
//    }
//
//    companion object {
//        private var local_playlists: MutableList<LocalPlaylist>? = null
//        private val load_mutex = Mutex()
//        private val playlists_listeners: MutableList<Listener> = mutableListOf()
//
//        fun addPlaylistsListener(listener: Listener) {
//            playlists_listeners.addUnique(listener)
//        }
//        fun removePlaylistsListener(listener: Listener) {
//            playlists_listeners.remove(listener)
//        }
//
//        @Composable
//        fun rememberLocalPlaylistsListener(): List<LocalPlaylist> {
//            val playlists: MutableList<LocalPlaylist> = remember { mutableStateListOf() }
//
//            LaunchedEffect(Unit) {
//                playlists.addAll(getLocalPlaylists(SpMp.context).toMutableStateList())
//            }
//            DisposableEffect(Unit) {
//                val listener = object : Listener {
//                    override fun onAdded(playlist: LocalPlaylist) {
//                        playlists.add(playlist)
//                    }
//                    override fun onRemoved(index: Int, playlist: LocalPlaylist) {
//                        playlists.removeAt(index)
//                    }
//                }
//                addPlaylistsListener(listener)
//                onDispose {
//                    removePlaylistsListener(listener)
//                }
//            }
//
//            return playlists
//        }
//
//        suspend fun fromId(id: String, context: PlatformContext = SpMp.context): LocalPlaylist {
//            getLocalPlaylists(context)
//            load_mutex.withLock {
//                return local_playlists!!.first { it.id == id }
//            }
//        }
//
//        suspend fun getLocalPlaylists(context: PlatformContext): List<LocalPlaylist> {
//            load_mutex.withLock {
//                local_playlists?.also {
//                    return it.toList()
//                }
//
//                return withContext(Dispatchers.IO) {
//                    val dir = getPlaylistsDirectory(context)
//                    if (dir.isDirectory) {
//                        val files = dir.listFiles() ?: emptyArray()
//                        local_playlists = files.map { file ->
//                            LocalPlaylist(file.name, context).apply { loadFromCache() }
//                        }.toMutableList()
//                    }
//                    else {
//                        local_playlists = mutableListOf()
//                    }
//
//                    return@withContext local_playlists!!.toList()
//                }
//            }
//        }
//
//        suspend fun createLocalPlaylist(context: PlatformContext): LocalPlaylist = withContext(Dispatchers.IO) {
//            getLocalPlaylists(context)
//
//            load_mutex.withLock {
//                var id: Int = 0
//                for (playlist in local_playlists ?: emptyList()) {
//                    id = maxOf(id, playlist.id.toInt() + 1)
//                }
//
//                val file = getPlaylistFileFromId(context, id.toString())
//                check(!file.exists())
//
//                val playlist = LocalPlaylist(id.toString(), context)
//                playlist.editData {
//                    supplyTitle(getString("new_playlist_title"))
//                }
//                local_playlists!!.add(playlist)
//
//                for (listener in playlists_listeners) {
//                    listener.onAdded(playlist)
//                }
//
//                return@withContext playlist
//            }
//        }
//
//        private suspend fun deleteLocalPlaylist(context: PlatformContext, id: String) = withContext(Dispatchers.IO) {
//            getLocalPlaylists(context)
//
//            load_mutex.withLock {
//                val file = getPlaylistFileFromId(context, id)
//                check(file.exists())
//
//                val index = local_playlists!!.indexOfFirst { it.id == id }
//                val playlist = local_playlists!!.removeAt(index)
//
//                assert(file.delete())
//
//                playlist.registry_entry.clear()
//                playlist.saveRegistry()
//
//                for (listener in playlists_listeners) {
//                    listener.onRemoved(index, playlist)
//                }
//            }
//        }
//    }
//}
//
//private fun getPlaylistsDirectory(context: PlatformContext): File =
//    context.getFilesDir().resolve("localPlaylists")
//
//private fun getPlaylistFileFromId(context: PlatformContext, id: String): File = getPlaylistsDirectory(context).resolve(id)
//
//private class LocalPlaylistItemData(item: LocalPlaylist): PlaylistItemData(item) {
//    override var items: MutableList<MediaItem>? = mutableStateListOf()
//
//    override fun saveData(data: String) {
//        val file = getPlaylistFileFromId(SpMp.context, data_item.id)
//        file.parentFile.mkdirs()
//        file.writeText(data)
//    }
//
//    override fun getDataReader(): Reader? {
//        val file = getPlaylistFileFromId(SpMp.context, data_item.id)
//        if (!file.isFile) {
//            return null
//        }
//        return file.reader()
//    }
//}
