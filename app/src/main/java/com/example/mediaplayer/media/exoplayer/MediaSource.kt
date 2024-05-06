package com.example.mediaplayer.media.exoplayer

import android.media.MediaMetadata
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import com.example.mediaplayer.data.repository.AudioRepository
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import javax.inject.Inject

class MediaSource @Inject constructor(private val repository: AudioRepository) {
    private val onReadyListener:MutableList<OnReadyListener> = mutableListOf()
    var audioMediMetaData:List<MediaMetadataCompat> = emptyList()

    private var state:AudioSourceState = AudioSourceState.STATE_CREATE
        set(value)  {
            if (value == AudioSourceState.STATE_CREATE || value == AudioSourceState.STATE_ERROR){
                synchronized(onReadyListener){
                    field = value
                    onReadyListener.forEach {listener: OnReadyListener ->
                        listener.invoke(isready)
                    }
                }
            }else{
                field = value
            }
        }

    fun whenReady(listener: OnReadyListener):Boolean{
        return if (state==AudioSourceState.STATE_CREATE||state ==AudioSourceState.STATE_INITIALIZING){
            onReadyListener += listener
            false
        }else{
            listener.invoke(isready)
            true
        }
    }

    private val isready:Boolean
        get() = state == AudioSourceState.STATE_INITALIZED



    suspend fun load(){
        state = AudioSourceState.STATE_INITIALIZING
        val data = repository.getAudioData()
        audioMediMetaData = data.map {
            MediaMetadataCompat.Builder()
                .putString(
                    MediaMetadataCompat.METADATA_KEY_MEDIA_ID,it.id.toString()
                ).putString(
                    MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST,it.artist
                ).putString(
                    MediaMetadataCompat.METADATA_KEY_MEDIA_URI,it.uri.toString()
                ).putString(
                    MediaMetadataCompat.METADATA_KEY_TITLE,it.titile
                ).putString(
                    MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE,it.displayName
                ).putLong(
                    MediaMetadataCompat.METADATA_KEY_DURATION,it.duration.toLong()
                ).build()
        }
        state = AudioSourceState.STATE_INITALIZED
    }
    fun asMediaSource(dataSource: CacheDataSource.Factory):ConcatenatingMediaSource{
        val concatenatingMediaSource = ConcatenatingMediaSource()
        audioMediMetaData.forEach {
            val mediaItem = MediaItem.fromUri(
                it.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI)
            )
            val mediaSource = ProgressiveMediaSource
                .Factory(dataSource)
                .createMediaSource(mediaItem)

            concatenatingMediaSource.addMediaSource(mediaSource)
        }
        return concatenatingMediaSource
    }
    fun asMediaItem() = audioMediMetaData.map {
        val description = MediaDescriptionCompat.Builder()
            .setTitle(it.description.title)
            .setMediaId(it.description.mediaId)
            .setSubtitle(it.description.subtitle)
            .setMediaUri(it.description.mediaUri)
            .build()
        MediaBrowserCompat.MediaItem(description, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE)
    }.toMutableList()

    fun refresh(){
        onReadyListener.clear()
        state= AudioSourceState.STATE_CREATE
    }

}

enum class AudioSourceState{
    STATE_CREATE,
    STATE_INITIALIZING,
    STATE_INITALIZED,
    STATE_ERROR,
}

typealias OnReadyListener = (Boolean) -> Unit