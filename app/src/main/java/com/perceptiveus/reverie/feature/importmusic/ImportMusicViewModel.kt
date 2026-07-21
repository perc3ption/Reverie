package com.perceptiveus.reverie.feature.importmusic

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.perceptiveus.reverie.core.entitlement.FeatureAccessChecker
import com.perceptiveus.reverie.data.import.MusicImportRepository
import com.perceptiveus.reverie.data.repository.MusicLibraryRepository
import com.perceptiveus.reverie.data.storage.ImportMode
import com.perceptiveus.reverie.data.storage.MusicLibraryStorage
import com.perceptiveus.reverie.domain.model.ImportResult
import com.perceptiveus.reverie.domain.model.LibraryScanResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ImportMusicViewModel(
    private val musicLibraryRepository: MusicLibraryRepository,
    private val musicImportRepository: MusicImportRepository,
    private val musicLibraryStorage: MusicLibraryStorage,
    private val featureAccessChecker: FeatureAccessChecker,
) : ViewModel() {

    val songCount: StateFlow<Int> = musicLibraryRepository.songCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage

    private val _importMode = MutableStateFlow(ImportMode.COPY)
    val importMode: StateFlow<ImportMode> = _importMode

    private val _destinationRelativePath = MutableStateFlow("")
    val destinationRelativePath: StateFlow<String> = _destinationRelativePath

    private val _showImportOptions = MutableStateFlow(false)
    val showImportOptions: StateFlow<Boolean> = _showImportOptions

    private val _pickerType = MutableStateFlow(ImportPickerType.FILES)
    val pickerType: StateFlow<ImportPickerType> = _pickerType

    private val _showDestinationPicker = MutableStateFlow(false)
    val showDestinationPicker: StateFlow<Boolean> = _showDestinationPicker

    val storage: MusicLibraryStorage get() = musicLibraryStorage

    fun isPremium(): Boolean = featureAccessChecker.isPremium()

    fun openImportOptions() {
        _showImportOptions.value = true
    }

    fun dismissImportOptions() {
        _showImportOptions.value = false
    }

    fun setPickerType(type: ImportPickerType) {
        _pickerType.value = type
    }

    fun setImportMode(mode: ImportMode) {
        _importMode.value = mode
    }

    fun openDestinationPicker() {
        _showDestinationPicker.value = true
    }

    fun dismissDestinationPicker() {
        _showDestinationPicker.value = false
    }

    fun selectDestination(relativePath: String) {
        _destinationRelativePath.value = relativePath
        _showDestinationPicker.value = false
        try {
            musicLibraryStorage.createSubdirectory(relativePath)
        } catch (_: Exception) {
            _statusMessage.value = "Could not create destination folder."
        }
    }

    fun createDestinationAndSelect(relativePath: String) {
        try {
            musicLibraryStorage.createSubdirectory(relativePath)
            _destinationRelativePath.value = relativePath
            _showDestinationPicker.value = false
        } catch (e: Exception) {
            _statusMessage.value = "Could not create folder: ${e.message ?: "Unknown error"}"
        }
    }

    fun scanLibrary() {
        viewModelScope.launch {
            _isBusy.value = true
            _statusMessage.value = null
            try {
                val result = musicLibraryRepository.scanLibrary()
                _statusMessage.value = formatScanResult(result)
            } catch (e: Exception) {
                _statusMessage.value = "Scan failed: ${e.message ?: "Unknown error"}"
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun importSongUris(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _isBusy.value = true
            _statusMessage.value = null
            try {
                val result = musicImportRepository.importFiles(
                    uris = uris,
                    mode = _importMode.value,
                    destinationRelativePath = _destinationRelativePath.value,
                )
                _statusMessage.value = formatImportResult(result)
            } catch (e: Exception) {
                _statusMessage.value = "Import failed: ${e.message ?: "Unknown error"}"
            } finally {
                _isBusy.value = false
                _showImportOptions.value = false
            }
        }
    }

    fun importFolderUri(treeUri: Uri) {
        viewModelScope.launch {
            _isBusy.value = true
            _statusMessage.value = null
            try {
                val result = musicImportRepository.importFolderTree(
                    treeUri = treeUri,
                    mode = _importMode.value,
                    destinationRelativePath = _destinationRelativePath.value,
                )
                _statusMessage.value = formatImportResult(result)
            } catch (e: Exception) {
                _statusMessage.value = "Import failed: ${e.message ?: "Unknown error"}"
            } finally {
                _isBusy.value = false
                _showImportOptions.value = false
            }
        }
    }

    private fun formatScanResult(result: LibraryScanResult): String {
        val parts = mutableListOf<String>()
        parts += "Indexed ${result.tracksIndexed} song(s) in ${result.foldersIndexed} folder(s)."
        if (result.tracksUnchanged > 0) {
            parts += "${result.tracksUnchanged} unchanged (skipped re-read)."
        }
        if (result.tracksRemoved > 0) {
            parts += "Removed ${result.tracksRemoved} missing entr(ies)."
        }
        if (result.skippedUnreadable > 0) {
            parts += "Skipped ${result.skippedUnreadable} unreadable file(s)."
        }
        if (result.truncatedBySongLimit) {
            parts += "Free tier limit reached — upgrade for unlimited library."
        }
        return parts.joinToString(" ")
    }

    private fun formatImportResult(result: ImportResult): String {
        val parts = mutableListOf<String>()
        parts += "Imported ${result.filesImported} file(s)."
        if (result.filesFailed > 0) {
            parts += "${result.filesFailed} failed."
        }
        if (result.moveDeleteFailures > 0) {
            parts += "${result.moveDeleteFailures} source file(s) could not be removed after move."
        }
        if (result.truncatedBySongLimit) {
            parts += "Free tier song limit reached."
        }
        result.scanResult?.let { scan ->
            parts += "Library now has ${scan.tracksIndexed} indexed song(s)."
        }
        return parts.joinToString(" ")
    }
}
