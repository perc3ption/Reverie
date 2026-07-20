package com.perceptiveus.reverie.feature.tutorial

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.perceptiveus.reverie.core.settings.SettingsRepository
import com.perceptiveus.reverie.data.repository.MusicLibraryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TutorialHubUiState(
    val progress: TutorialProgress = TutorialProgress.Default,
    val songCount: Int = 0,
    val showFirstRun: Boolean = false,
)

class TutorialViewModel(
    private val settingsRepository: SettingsRepository,
    musicLibraryRepository: MusicLibraryRepository,
) : ViewModel() {

    val chapters: List<TutorialChapter> = TutorialCatalog.chapters

    val progress: StateFlow<TutorialProgress> = settingsRepository.tutorialProgress
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TutorialProgress.Default)

    val hubState: StateFlow<TutorialHubUiState> = combine(
        settingsRepository.tutorialProgress,
        musicLibraryRepository.songCount,
    ) { progress, songCount ->
        TutorialHubUiState(
            progress = progress,
            songCount = songCount,
            showFirstRun = !progress.firstRunDismissed && songCount == 0,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        TutorialHubUiState(),
    )

    fun completedCount(): Int = progress.value.completedChapterIds.size

    fun markChapterCompleted(chapterId: String) {
        viewModelScope.launch {
            val current = settingsRepository.tutorialProgress.value
            if (chapterId in current.completedChapterIds) return@launch
            settingsRepository.setTutorialProgress(current.withCompleted(chapterId))
        }
    }

    fun dismissFirstRun() {
        viewModelScope.launch {
            val current = settingsRepository.tutorialProgress.value
            settingsRepository.setTutorialProgress(current.copy(firstRunDismissed = true))
        }
    }

    fun chapter(id: String): TutorialChapter? = TutorialCatalog.byId(id)
}
