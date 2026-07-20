package com.perceptiveus.reverie.feature.smartplaylist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.perceptiveus.reverie.data.repository.SmartPlaylistAccessException
import com.perceptiveus.reverie.data.repository.SmartPlaylistRepository
import com.perceptiveus.reverie.data.repository.SongTagRepository
import com.perceptiveus.reverie.domain.model.SmartPlaylistField
import com.perceptiveus.reverie.domain.model.SmartPlaylistOperator
import com.perceptiveus.reverie.domain.model.SmartPlaylistRule
import com.perceptiveus.reverie.domain.model.SmartPlaylistSort
import com.perceptiveus.reverie.domain.model.Tag
import com.perceptiveus.reverie.domain.model.operatorsFor
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class EditableSmartRule(
    val key: Long,
    val field: SmartPlaylistField = SmartPlaylistField.ARTIST,
    val operator: SmartPlaylistOperator = SmartPlaylistOperator.CONTAINS,
    val value: String = "",
    val valueSecondary: String = "",
)

class SmartPlaylistEditorViewModel(
    private val playlistId: String?,
    private val smartPlaylistRepository: SmartPlaylistRepository,
    songTagRepository: SongTagRepository,
) : ViewModel() {

    val isEditing: Boolean = !playlistId.isNullOrBlank()

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    private val _sortOrder = MutableStateFlow(SmartPlaylistSort.TITLE)
    val sortOrder: StateFlow<SmartPlaylistSort> = _sortOrder.asStateFlow()

    private val _trackLimit = MutableStateFlow("100")
    val trackLimit: StateFlow<String> = _trackLimit.asStateFlow()

    private val _rules = MutableStateFlow(
        listOf(EditableSmartRule(key = 1L)),
    )
    val rules: StateFlow<List<EditableSmartRule>> = _rules.asStateFlow()

    val availableTags: StateFlow<List<Tag>> = songTagRepository.observeAllTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _userMessages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val userMessages: SharedFlow<String> = _userMessages.asSharedFlow()

    private val _savedPlaylistId = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val savedPlaylistId: SharedFlow<String> = _savedPlaylistId.asSharedFlow()

    private var nextRuleKey = 2L

    init {
        if (!playlistId.isNullOrBlank()) {
            viewModelScope.launch {
                val playlist = smartPlaylistRepository.getPlaylist(playlistId) ?: return@launch
                val loadedRules = smartPlaylistRepository.getRules(playlistId)
                _name.value = playlist.name
                _sortOrder.value = playlist.sortOrder
                _trackLimit.value = playlist.trackLimit.toString()
                if (loadedRules.isNotEmpty()) {
                    _rules.value = loadedRules.mapIndexed { index, rule ->
                        EditableSmartRule(
                            key = index + 1L,
                            field = rule.field,
                            operator = rule.operator,
                            value = rule.value,
                            valueSecondary = rule.valueSecondary,
                        )
                    }
                    nextRuleKey = loadedRules.size + 1L
                }
            }
        }
    }

    fun setName(value: String) {
        _name.value = value
    }

    fun setSortOrder(value: SmartPlaylistSort) {
        _sortOrder.value = value
    }

    fun setTrackLimit(value: String) {
        _trackLimit.value = value.filter { it.isDigit() }.take(3)
    }

    fun addRule() {
        val key = nextRuleKey++
        _rules.value = _rules.value + EditableSmartRule(key = key)
    }

    fun removeRule(key: Long) {
        val current = _rules.value
        if (current.size <= 1) {
            viewModelScope.launch { _userMessages.emit("Keep at least one rule.") }
            return
        }
        _rules.value = current.filterNot { it.key == key }
    }

    fun updateRule(key: Long, transform: (EditableSmartRule) -> EditableSmartRule) {
        _rules.value = _rules.value.map { rule ->
            if (rule.key != key) rule else transform(rule)
        }
    }

    fun setField(key: Long, field: SmartPlaylistField) {
        updateRule(key) { rule ->
            val ops = operatorsFor(field)
            rule.copy(
                field = field,
                operator = ops.firstOrNull() ?: SmartPlaylistOperator.CONTAINS,
                value = "",
                valueSecondary = "",
            )
        }
    }

    fun save() {
        viewModelScope.launch {
            val limit = _trackLimit.value.toIntOrNull()?.coerceIn(1, 500) ?: 100
            val domainRules = _rules.value.map { rule ->
                SmartPlaylistRule(
                    field = rule.field,
                    operator = rule.operator,
                    value = rule.value.trim(),
                    valueSecondary = rule.valueSecondary.trim(),
                )
            }
            val result = if (playlistId.isNullOrBlank()) {
                smartPlaylistRepository.createPlaylist(
                    name = _name.value,
                    sortOrder = _sortOrder.value,
                    trackLimit = limit,
                    rules = domainRules,
                )
            } else {
                smartPlaylistRepository.updatePlaylist(
                    id = playlistId,
                    name = _name.value,
                    sortOrder = _sortOrder.value,
                    trackLimit = limit,
                    rules = domainRules,
                )
            }
            result
                .onSuccess { playlist ->
                    _userMessages.emit(if (isEditing) "Smart playlist updated." else "Smart playlist created.")
                    _savedPlaylistId.emit(playlist.id)
                }
                .onFailure { error ->
                    val message = when (error) {
                        is SmartPlaylistAccessException -> error.message
                        else -> error.message
                    } ?: "Could not save smart playlist."
                    _userMessages.emit(message)
                }
        }
    }

    companion object {
        fun factory(
            playlistId: String?,
            smartPlaylistRepository: SmartPlaylistRepository,
            songTagRepository: SongTagRepository,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SmartPlaylistEditorViewModel(
                    playlistId = playlistId,
                    smartPlaylistRepository = smartPlaylistRepository,
                    songTagRepository = songTagRepository,
                ) as T
            }
        }
    }
}
