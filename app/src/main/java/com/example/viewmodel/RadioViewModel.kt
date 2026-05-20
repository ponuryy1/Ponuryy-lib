package com.example.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.data.AppDatabase
import com.example.data.Station
import com.example.data.StationRepository
import com.example.service.RadioPlaybackService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class RadioViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: StationRepository
    private var mediaController: MediaController? = null

    // State flows
    private val _allStations = MutableStateFlow<List<Station>>(emptyList())
    
    val searchQuery = MutableStateFlow("")
    val selectedGenre = MutableStateFlow("Всі")
    val isFavoriteOnly = MutableStateFlow(false)

    // Exposed filtered stations
    val stations: StateFlow<List<Station>> = combine(
        _allStations,
        searchQuery,
        selectedGenre,
        isFavoriteOnly
    ) { list, query, genre, favOnly ->
        list.filter { station ->
            val matchesQuery = station.name.contains(query, ignoreCase = true) ||
                    station.genre.contains(query, ignoreCase = true)
            val matchesGenre = genre == "Всі" || station.genre == genre
            val matchesFav = !favOnly || station.isFavorite
            matchesQuery && matchesGenre && matchesFav
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All available genres for clean chips navigation
    val genres: StateFlow<List<String>> = _allStations.map { list ->
        val standardGenres = list.map { it.genre }.distinct().sorted()
        listOf("Всі") + standardGenres
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("Всі"))

    // Player state
    private val _currentPlayingStation = MutableStateFlow<Station?>(null)
    val currentPlayingStation: StateFlow<Station?> = _currentPlayingStation.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Timer state
    private val _sleepTimerRemainingSeconds = MutableStateFlow(0)
    val sleepTimerRemainingSeconds: StateFlow<Int> = _sleepTimerRemainingSeconds.asStateFlow()

    private var sleepTimerJob: Job? = null

    init {
        val database = AppDatabase.getDatabase(application)
        repository = StationRepository(database.stationDao())

        viewModelScope.launch {
            // Prepopulate if DB is empty
            repository.prepopulateIfEmpty()
            
            // Connect to Room DB
            repository.allStations.collect { list ->
                _allStations.value = list
                
                // Keep playing reference up-to-date with DB changes
                _currentPlayingStation.value?.let { current ->
                    val updated = list.find { it.id == current.id }
                    if (updated != null) {
                        _currentPlayingStation.value = updated
                    }
                }
            }
        }
    }

    /**
     * Connects to the background MediaPlaybackService using Media3 controller.
     */
    fun initController(context: Context) {
        if (mediaController != null) return

        val sessionToken = SessionToken(
            context,
            ComponentName(context, RadioPlaybackService::class.java)
        )
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        
        controllerFuture.addListener({
            try {
                val controller = controllerFuture.get()
                mediaController = controller
                
                // Sync status with ExoPlayer
                controller.addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlayingChanged: Boolean) {
                        _isPlaying.value = isPlayingChanged
                        _isLoading.value = controller.playbackState == Player.STATE_BUFFERING
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        _isLoading.value = playbackState == Player.STATE_BUFFERING
                        _isPlaying.value = controller.isPlaying
                    }
                })

                // Set initial values
                _isPlaying.value = controller.isPlaying
                _isLoading.value = controller.playbackState == Player.STATE_BUFFERING

                // Recover now playing station from current controller URL if applicable
                controller.currentMediaItem?.let { item ->
                    val matched = _allStations.value.find { it.streamUrl == item.localConfiguration?.uri.toString() }
                    if (matched != null) {
                        _currentPlayingStation.value = matched
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, context.mainExecutor)
    }

    /**
     * Set a custom sleep timer to turn off playback.
     */
    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes == 0) {
            _sleepTimerRemainingSeconds.value = 0
            return
        }

        _sleepTimerRemainingSeconds.value = minutes * 60
        sleepTimerJob = viewModelScope.launch {
            while (_sleepTimerRemainingSeconds.value > 0) {
                delay(1000)
                _sleepTimerRemainingSeconds.value -= 1
            }
            pausePlayback()
        }
    }

    fun playStation(station: Station) {
        _currentPlayingStation.value = station
        mediaController?.let { controller ->
            val mediaItem = MediaItem.Builder()
                .setMediaId(station.id.toString())
                .setUri(station.streamUrl)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(station.name)
                        .setGenre(station.genre)
                        .setArtworkUri(Uri.parse(station.iconUrl))
                        .build()
                )
                .build()
            
            controller.setMediaItem(mediaItem)
            controller.prepare()
            controller.play()
        }
    }

    fun togglePlayPause() {
        mediaController?.let { controller ->
            if (controller.isPlaying) {
                controller.pause()
            } else {
                val station = _currentPlayingStation.value
                if (station != null) {
                    // Check if URL matches current item URI
                    val currentUri = controller.currentMediaItem?.localConfiguration?.uri?.toString()
                    if (currentUri == station.streamUrl) {
                        controller.play()
                    } else {
                        playStation(station)
                    }
                } else {
                    _allStations.value.firstOrNull()?.let { playStation(it) }
                }
            }
        }
    }

    fun pausePlayback() {
        mediaController?.pause()
    }

    fun toggleFavorite(station: Station) {
        viewModelScope.launch {
            val updated = station.copy(isFavorite = !station.isFavorite)
            repository.update(updated)
        }
    }

    fun addCustomStation(name: String, streamUrl: String, genre: String, iconUrl: String?) {
        viewModelScope.launch {
            val defaultIcons = listOf(
                "https://static.onlineradiobox.com/img/logo/8/7208.v15.png",
                "https://static.onlineradiobox.com/img/logo/9/7209.v21.png",
                "https://static.onlineradiobox.com/img/logo/0/7210.v16.png",
                "https://static.onlineradiobox.com/img/logo/3/13503.v14.png"
            )
            val actualIcon = if (iconUrl.isNullOrBlank()) {
                defaultIcons.random()
            } else {
                iconUrl
            }
            val custom = Station(
                name = name,
                streamUrl = streamUrl,
                iconUrl = actualIcon,
                genre = if (genre.isBlank()) "Моє радіо" else genre,
                isCustom = true
            )
            repository.insert(custom)
        }
    }

    fun deleteStation(station: Station) {
        viewModelScope.launch {
            repository.delete(station)
            if (_currentPlayingStation.value?.id == station.id) {
                mediaController?.stop()
                _currentPlayingStation.value = null
            }
        }
    }

    fun syncWithLatestStations(onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.syncWithLatestStations()
            onComplete()
        }
    }

    override fun onCleared() {
        super.onCleared()
        // We let the MediaSessionService handle player lifecycle when swipe-dismissed.
        mediaController?.release()
    }
}
