package com.perceptiveus.reverie.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.perceptiveus.reverie.core.entitlement.AppFeature
import com.perceptiveus.reverie.core.entitlement.FeatureAccessChecker
import com.perceptiveus.reverie.data.repository.MusicLibraryRepository
import com.perceptiveus.reverie.data.repository.PlaybackRepository
import com.perceptiveus.reverie.data.repository.PlaylistLimitException
import com.perceptiveus.reverie.data.repository.PlaylistRepository
import com.perceptiveus.reverie.domain.model.Album
import com.perceptiveus.reverie.domain.model.Artist
import com.perceptiveus.reverie.domain.model.MusicFolder
import com.perceptiveus.reverie.domain.model.Playlist
import com.perceptiveus.reverie.domain.model.QueueSource
import com.perceptiveus.reverie.domain.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class LibraryTab {
    FOLDERS,
    PLAYLISTS,
    ARTISTS,
    ALBUMS,
}

data class FolderBrowserState(
    val path: String = "",
    val title: String = "Library Root",
    val breadcrumb: String = "Reverie",
    val canNavigateUp: Boolean = false,
    val childFolders: List<MusicFolder> = emptyList(),
    val songs: List<Track> = emptyList(),
    /** All songs in this folder and nested subfolders (for Play All). */
    val subtreeSongs: List<Track> = emptyList(),
)

data class ArtistBrowserState(
    val selectedArtist: String? = null,
    val songs: List<Track> = emptyList(),
)

data class AlbumBrowserState(
    val selectedAlbum: Album? = null,
    val songs: List<Track> = emptyList(),
)

class LibraryViewModel(
    musicLibraryRepository: MusicLibraryRepository,
    private val playlistRepository: PlaylistRepository,
    private val playbackRepository: PlaybackRepository,
    private val featureAccessChecker: FeatureAccessChecker,
) : ViewModel() {

    val songs: StateFlow<List<Track>> = musicLibraryRepository.songs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val folders: StateFlow<List<MusicFolder>> = musicLibraryRepository.folders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val artists: StateFlow<List<Artist>> = musicLibraryRepository.artists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val albums: StateFlow<List<Album>> = musicLibraryRepository.albums
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val playlists: StateFlow<List<Playlist>> = playlistRepository.playlists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _folderPath = MutableStateFlow("")
    val folderBrowser: StateFlow<FolderBrowserState> = combine(
        folders,
        songs,
        _folderPath,
    ) { allFolders, allSongs, path ->
        buildFolderBrowserState(allFolders, allSongs, path)
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FolderBrowserState())

    private val _selectedArtistName = MutableStateFlow<String?>(null)

    val artistBrowser: StateFlow<ArtistBrowserState> = combine(
        songs,
        _selectedArtistName,
    ) { allSongs, artistName ->
        if (artistName == null) {
            ArtistBrowserState()
        } else {
            ArtistBrowserState(
                selectedArtist = artistName,
                songs = allSongs
                    .filter { it.artist.equals(artistName, ignoreCase = true) }
                    .sortedBy { it.title.lowercase() },
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ArtistBrowserState())

    private val _selectedAlbumKey = MutableStateFlow<String?>(null)

    val albumBrowser: StateFlow<AlbumBrowserState> = combine(
        albums,
        songs,
        _selectedAlbumKey,
    ) { allAlbums, allSongs, albumKey ->
        if (albumKey == null) {
            AlbumBrowserState()
        } else {
            val album = allAlbums.firstOrNull { it.id == albumKey }
            if (album == null) {
                AlbumBrowserState()
            } else {
                AlbumBrowserState(
                    selectedAlbum = album,
                    songs = allSongs
                        .filter {
                            it.album.equals(album.title, ignoreCase = true) &&
                                it.artist.equals(album.artist, ignoreCase = true)
                        }
                        .sortedBy { it.title.lowercase() },
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AlbumBrowserState())

    private val _showAllSongs = MutableStateFlow(false)
    val showAllSongs: StateFlow<Boolean> = _showAllSongs.asStateFlow()

    private val _userMessages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val userMessages: SharedFlow<String> = _userMessages.asSharedFlow()

    fun openAllSongs() {
        _showAllSongs.value = true
    }

    fun closeAllSongs() {
        _showAllSongs.value = false
    }

    fun playAllSongs() {
        val all = songs.value
        if (all.isEmpty()) return
        playbackRepository.play(all, 0, QueueSource.Library)
    }

    fun playPlaylist(playlist: Playlist) {
        viewModelScope.launch {
            val tracks = playlistRepository.observePlaylistTracks(playlist.id).first()
            if (tracks.isEmpty()) {
                _userMessages.emit("Playlist is empty.")
                return@launch
            }
            playbackRepository.play(
                tracks,
                0,
                QueueSource.Playlist(
                    name = playlist.name,
                    description = playlist.description,
                    coverPath = playlist.coverPath,
                ),
            )
        }
    }

    /** Handles system / edge back for in-Library drill-downs. Returns true if consumed. */
    fun handleLibraryBack(): Boolean {
        if (_showAllSongs.value) {
            _showAllSongs.value = false
            return true
        }
        if (_selectedArtistName.value != null) {
            _selectedArtistName.value = null
            return true
        }
        if (_selectedAlbumKey.value != null) {
            _selectedAlbumKey.value = null
            return true
        }
        if (_folderPath.value.isNotEmpty()) {
            navigateFolderUp()
            return true
        }
        return false
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            playlistRepository.createPlaylist(name).fold(
                onSuccess = { _userMessages.emit("Playlist \"${it.name}\" created.") },
                onFailure = { error ->
                    val message = when (error) {
                        is PlaylistLimitException ->
                            "Free tier allows ${FeatureAccessChecker.FREE_MAX_PLAYLISTS} playlists."
                        else -> error.message ?: "Could not create playlist."
                    }
                    _userMessages.emit(message)
                },
            )
        }
    }

    fun deletePlaylist(playlist: Playlist) {
        viewModelScope.launch {
            playlistRepository.deletePlaylist(playlist.id)
            _userMessages.emit("Deleted \"${playlist.name}\".")
        }
    }

    fun openFolder(relativePath: String) {
        _folderPath.value = relativePath
    }

    fun navigateFolderUp() {
        val path = _folderPath.value
        _folderPath.value = if ('/' in path) path.substringBeforeLast('/') else ""
    }

    fun openArtist(artistName: String) {
        _selectedArtistName.value = artistName
    }

    fun clearSelectedArtist() {
        _selectedArtistName.value = null
    }

    fun openAlbum(album: Album) {
        _selectedAlbumKey.value = album.id
    }

    fun clearSelectedAlbum() {
        _selectedAlbumKey.value = null
    }

    fun playSong(track: Track) {
        val allSongs = songs.value
        val index = allSongs.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        playbackRepository.play(allSongs, index, QueueSource.Library)
    }

    fun addToQueue(track: Track) {
        if (playbackRepository.addToQueue(listOf(track)) > 0) {
            viewModelScope.launch {
                _userMessages.emit("Added to queue")
            }
        }
    }

    fun playSongInFolder(track: Track) {
        val folder = folderBrowser.value
        val folderSongs = folder.songs
        if (folderSongs.isEmpty()) {
            playSong(track)
            return
        }
        val index = folderSongs.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        playbackRepository.play(folderSongs, index, QueueSource.Folder(folder.title))
    }

    fun playAllInCurrentFolder(): Boolean {
        val folder = folderBrowser.value
        val queue = folder.subtreeSongs
        if (queue.isEmpty()) return false
        playbackRepository.play(queue, 0, QueueSource.Folder(folder.title))
        return true
    }

    fun playSongByArtist(track: Track) {
        val artistName = artistBrowser.value.selectedArtist ?: track.artist
        val artistSongs = artistBrowser.value.songs
        if (artistSongs.isEmpty()) {
            playSong(track)
            return
        }
        val index = artistSongs.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        playbackRepository.play(artistSongs, index, QueueSource.Artist(artistName, artworkFrom(artistSongs)))
    }

    fun playAllArtistSongs(): Boolean {
        val artistName = artistBrowser.value.selectedArtist ?: return false
        val queue = artistBrowser.value.songs
        if (queue.isEmpty()) return false
        playbackRepository.play(queue, 0, QueueSource.Artist(artistName, artworkFrom(queue)))
        return true
    }

    fun playAllForArtist(artistName: String): Boolean {
        val queue = songs.value
            .filter { it.artist.equals(artistName, ignoreCase = true) }
            .sortedBy { it.title.lowercase() }
        if (queue.isEmpty()) return false
        playbackRepository.play(queue, 0, QueueSource.Artist(artistName, artworkFrom(queue)))
        return true
    }

    fun playSongInAlbum(track: Track) {
        val album = albumBrowser.value.selectedAlbum
        val albumSongs = albumBrowser.value.songs
        if (albumSongs.isEmpty()) {
            playSong(track)
            return
        }
        val index = albumSongs.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        playbackRepository.play(
            albumSongs,
            index,
            albumSource(album?.title ?: track.album, album?.artist ?: track.artist, albumSongs),
        )
    }

    fun playAllAlbumSongs(): Boolean {
        val album = albumBrowser.value.selectedAlbum ?: return false
        val queue = albumBrowser.value.songs
        if (queue.isEmpty()) return false
        playbackRepository.play(queue, 0, albumSource(album.title, album.artist, queue))
        return true
    }

    fun playAllForAlbum(album: Album): Boolean {
        val queue = songs.value
            .filter {
                it.album.equals(album.title, ignoreCase = true) &&
                    it.artist.equals(album.artist, ignoreCase = true)
            }
            .sortedBy { it.title.lowercase() }
        if (queue.isEmpty()) return false
        playbackRepository.play(queue, 0, albumSource(album.title, album.artist, queue))
        return true
    }

    fun canAccess(feature: AppFeature): Boolean = featureAccessChecker.canAccess(feature)

    fun isPremium(): Boolean = featureAccessChecker.isPremium()

    fun notifyFeatureComingSoon(featureName: String) {
        viewModelScope.launch {
            _userMessages.emit("$featureName is coming soon.")
        }
    }

    private fun albumSource(title: String, artist: String, tracks: List<Track>): QueueSource.Album {
        val year = tracks.map { it.year }.filter { it > 0 }.distinct().let { years ->
            years.singleOrNull() ?: years.maxOrNull() ?: 0
        }
        return QueueSource.Album(
            title = title,
            artist = artist,
            year = year,
            artworkPath = artworkFrom(tracks),
        )
    }

    private fun artworkFrom(tracks: List<Track>): String =
        tracks.firstOrNull { it.artworkPath.isNotBlank() }?.artworkPath.orEmpty()

    private fun buildFolderBrowserState(
        allFolders: List<MusicFolder>,
        allSongs: List<Track>,
        path: String,
    ): FolderBrowserState {
        val current = allFolders.firstOrNull { it.relativePath == path }
        val songsByFolderId = allSongs.groupBy { effectiveFolderId(it) }
        val subtreeAggByPath = buildSubtreeAggregates(allFolders, songsByFolderId)

        val childFolders = allFolders
            .asSequence()
            .filter { isDirectChildFolder(parentPath = path, childPath = it.relativePath) }
            .map { folder ->
                val agg = subtreeAggByPath[folder.relativePath]
                folder.copy(
                    songCount = agg?.songCount ?: 0,
                    albumCount = agg?.albumCount ?: 0,
                )
            }
            .sortedBy { it.name.lowercase() }
            .toList()

        val currentFolderId = current?.id
        val songsHere = when {
            currentFolderId != null -> songsByFolderId[currentFolderId].orEmpty()
            path.isEmpty() -> songsByFolderId[LIBRARY_ROOT_FOLDER_ID].orEmpty()
            else -> emptyList()
        }.sortedBy { it.title.lowercase() }

        val subtreeSongs = if (path.isEmpty()) {
            allSongs
        } else {
            collectSubtreeSongs(path, allFolders, songsByFolderId)
        }.sortedBy { it.title.lowercase() }

        return FolderBrowserState(
            path = path,
            title = current?.name ?: if (path.isEmpty()) "Library Root" else path.substringAfterLast('/'),
            breadcrumb = if (path.isEmpty()) "Reverie" else "Reverie/$path",
            canNavigateUp = path.isNotEmpty(),
            childFolders = childFolders,
            songs = songsHere,
            subtreeSongs = subtreeSongs,
        )
    }

    /**
     * One O(folders + songs) bottom-up pass: song/album counts for every folder path.
     * Avoids rescanning all songs once per child folder.
     */
    private fun buildSubtreeAggregates(
        allFolders: List<MusicFolder>,
        songsByFolderId: Map<String, List<Track>>,
    ): Map<String, FolderSubtreeAggregate> {
        val aggByPath = HashMap<String, FolderSubtreeAggregate>(allFolders.size.coerceAtLeast(1))
        for (folder in allFolders) {
            val direct = songsByFolderId[folder.id].orEmpty()
            var songCount = 0
            val albums = LinkedHashSet<String>()
            for (track in direct) {
                songCount++
                if (track.album.isNotBlank()) albums.add(track.album)
            }
            aggByPath[folder.relativePath] = FolderSubtreeAggregate(songCount, albums)
        }

        // Deepest paths first so parents receive fully rolled-up children.
        val deepFirst = allFolders.sortedByDescending { folderDepth(it.relativePath) }
        for (folder in deepFirst) {
            val childPath = folder.relativePath
            if (childPath.isEmpty()) continue
            val parentPath = parentFolderPath(childPath)
            val child = aggByPath[childPath] ?: continue
            val parent = aggByPath.getOrPut(parentPath) {
                FolderSubtreeAggregate(0, LinkedHashSet())
            }
            parent.songCount += child.songCount
            parent.albums.addAll(child.albums)
        }
        return aggByPath
    }

    private fun collectSubtreeSongs(
        folderPath: String,
        allFolders: List<MusicFolder>,
        songsByFolderId: Map<String, List<Track>>,
    ): List<Track> {
        val prefix = "$folderPath/"
        val out = ArrayList<Track>()
        for (folder in allFolders) {
            val rel = folder.relativePath
            if (rel == folderPath || rel.startsWith(prefix)) {
                out.addAll(songsByFolderId[folder.id].orEmpty())
            }
        }
        return out
    }

    private fun isDirectChildFolder(parentPath: String, childPath: String): Boolean {
        if (childPath.isEmpty()) return false
        if (parentPath.isEmpty()) return '/' !in childPath
        if (!childPath.startsWith("$parentPath/")) return false
        val rest = childPath.removePrefix("$parentPath/")
        return rest.isNotEmpty() && '/' !in rest
    }

    private fun effectiveFolderId(track: Track): String =
        track.folderId?.takeIf { it.isNotBlank() } ?: LIBRARY_ROOT_FOLDER_ID

    private fun parentFolderPath(relativePath: String): String =
        if ('/' in relativePath) relativePath.substringBeforeLast('/') else ""

    private fun folderDepth(relativePath: String): Int =
        if (relativePath.isEmpty()) 0 else relativePath.count { it == '/' } + 1

    private data class FolderSubtreeAggregate(
        var songCount: Int,
        val albums: MutableSet<String>,
    ) {
        val albumCount: Int get() = albums.size
    }

    private companion object {
        const val LIBRARY_ROOT_FOLDER_ID = "_library_root"
    }
}
