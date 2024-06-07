package com.example.mediaplayer.ui.audio

import android.support.v4.media.MediaBrowserCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mediaplayer.data.model.Audio
import com.example.mediaplayer.data.repository.AudioRepository
import com.example.mediaplayer.media.constants.AudioContanst
import com.example.mediaplayer.media.exoplayer.MediaPlayerServiceConnection
import com.example.mediaplayer.media.exoplayer.currentPosition
import com.example.mediaplayer.media.exoplayer.isPlaying
import com.example.mediaplayer.media.service.MediaPlayerService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AudioViewModel @Inject constructor(
    private val repository: AudioRepository,
    serviceConnection:MediaPlayerServiceConnection
):ViewModel() {
    var audioList = MutableStateFlow<List<Audio>?>(null)
    var audioLists = mutableListOf<Audio>()
    val currentPlayingAudio = serviceConnection.currentPlayingAudio
    private val isConnected = serviceConnection.isConnected
    lateinit var rootMediaId:String
    var currentPlaybackPotition = 0L
    private var updatePotition = true
    private val playBackState = serviceConnection.playBackState
    val isAudioPlaying :Boolean
        get() = playBackState.value?.isPlaying == true
    private val subcriptionCallback = object : MediaBrowserCompat.SubscriptionCallback(){
        override fun onChildrenLoaded(
            parentId: String,
            children: MutableList<MediaBrowserCompat.MediaItem>
        ) {
            super.onChildrenLoaded(parentId, children)
        }
    }
    private val serviceConnection = serviceConnection.also {
        updatePlayBack()
    }
    val currentDuration = MediaPlayerService.currentDuration
    var currentAudioProgress = MutableStateFlow(0f)

    init {
        viewModelScope.launch {
            audioLists += getAndFormatAudioData()
            audioList.value = audioLists
            isConnected.collect{
                if (it){
                    rootMediaId = serviceConnection.rootMediaId
                    serviceConnection.playBackState.value?.apply {
                        currentPlaybackPotition = position
                    }
                    serviceConnection.subcribe(rootMediaId,subcriptionCallback)

                }
            }
        }
    }

    private suspend fun getAndFormatAudioData():List<Audio>{
        return repository.getAudioData().map {
            val displayName = it.displayName.substringBefore(".")
            val artist = if (it.artist.contains("<unknown>")) "UnknowArtist" else it.artist
            it.copy(displayName=displayName, artist = artist)
        }
    }

    fun playAudio(currentAudio:Audio){
        serviceConnection.playAudio(audioLists)
        if (currentAudio.id == currentPlayingAudio.value?.id){
            if (isAudioPlaying){
                serviceConnection.transportControl.pause()
            }else{
                serviceConnection.transportControl.play()
            }
        }else{
            serviceConnection.transportControl.playFromMediaId(currentAudio.id.toString(),null)
        }
    }

    fun stopPlayBack(){
        serviceConnection.transportControl.stop()
    }
    fun fastForward(){
        serviceConnection.fastForWard()
    }
    fun rewind(){
        serviceConnection.rewind()
    }
    fun skipToNext(){
        serviceConnection.skipToNext()
    }
    fun seekTo(value:Float){
        serviceConnection.transportControl.seekTo((currentDuration*value/100f).toLong())
    }
    private fun updatePlayBack(){
        viewModelScope.launch {
            val potition = playBackState.value?.currentPosition ?: 0
            if (currentPlaybackPotition != potition){
                currentPlaybackPotition = potition
            }

            if (currentDuration > 0){
                currentAudioProgress.value = (currentPlaybackPotition.toFloat()/currentDuration.toFloat()*100f)
            }
            delay(AudioContanst.PLAYBACK_UPDATE_INTERVAL)
            if (updatePotition){
                updatePlayBack()
            }
        }

    }

    override fun onCleared() {
        super.onCleared()
        serviceConnection.unSubcribe(AudioContanst.MEDIA_ROOT_ID,object :MediaBrowserCompat.SubscriptionCallback(){})
        updatePotition =  false
    }

}