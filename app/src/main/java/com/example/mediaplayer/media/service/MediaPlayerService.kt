package com.example.mediaplayer.media.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.media.MediaDescription
import android.media.browse.MediaBrowser
import android.net.Uri
import android.os.Bundle
import android.os.ResultReceiver
import android.service.media.MediaBrowserService
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.content.ContextCompat
import androidx.media.MediaBrowserServiceCompat
import com.example.mediaplayer.media.constants.AudioContanst
import com.example.mediaplayer.media.exoplayer.MediaPlayerNotification
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.source.hls.DefaultHlsDataSourceFactory
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.example.mediaplayer.media.exoplayer.MediaSource
import com.google.android.exoplayer2.MediaMetadata
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MediaPlayerService : MediaBrowserServiceCompat() {

    @Inject
    lateinit var dataSourceFactory: CacheDataSource.Factory
    @Inject
    lateinit var exoPlayer:ExoPlayer

    @Inject
    lateinit var mediaSource : MediaSource

    private var serviceJob = SupervisorJob()
    private var serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private lateinit var mediaSession:MediaSessionCompat
    private lateinit var mediaSessionConnector: MediaSessionConnector

    private lateinit var mediaPlayerNotificationManager: MediaPlayerNotification
    private var currentPlayingMedia:MediaMetadataCompat? = null
    private val isPlayerInitialiez = false
    var isForegroundService = false

    companion object{
        private const val  TAG = "MediaPlayerService"
        var currentDuration :Long = 0L
            private set
    }

    override fun onCreate() {
        super.onCreate()
        val sessionActivityIntent = packageManager?.getLaunchIntentForPackage(packageName)
            ?.let { sessionIntent ->
                PendingIntent.getActivity(this,0,sessionIntent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            }

        mediaSession = MediaSessionCompat(this, TAG).apply {
            setSessionActivity(sessionActivityIntent)
            isActive = true
        }
        sessionToken = mediaSession.sessionToken
        mediaPlayerNotificationManager = MediaPlayerNotification(this,mediaSession.sessionToken,PlayerNotificationListener())
        serviceScope.launch {
            mediaSource.load()
        }
        mediaSessionConnector = MediaSessionConnector(mediaSession).apply {
            setPlaybackPreparer(AudioMediaPlayBackPrepare())
            setQueueNavigator(MediaQueueNavigator(mediaSession))
            setPlayer(exoPlayer)
        }
        mediaPlayerNotificationManager.showNotification(exoPlayer)
    }


    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot? {
        return BrowserRoot(AudioContanst.MEDIA_ROOT_ID,null)
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        when(parentId){
            AudioContanst.MEDIA_ROOT_ID -> {
                val resultsSent = mediaSource.whenReady {
                    if (it){
                        result.sendResult(mediaSource.asMediaItem())
                    }else{
                        result.sendResult(null)
                    }
                }
                if (!resultsSent){
                    result.detach()
                }
            }
            else -> Unit
        }
    }

    override fun onCustomAction(action: String, extras: Bundle?, result: Result<Bundle>) {
        when(action){
            AudioContanst.START_MEDIA_PLAY_ACTION -> {
                mediaPlayerNotificationManager.showNotification(exoPlayer)
            }
            AudioContanst.REFRESH_MEDIA_PLAY_ACTION ->{
                mediaSource.refresh()
                notifyChildrenChanged(AudioContanst.MEDIA_ROOT_ID)
            }
            else -> Unit
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        exoPlayer.release()
    }

    inner class MediaQueueNavigator(mediaSessionCompat:MediaSessionCompat) : TimelineQueueNavigator(mediaSessionCompat){
        override fun getMediaDescription(player: Player, windowIndex: Int): MediaDescriptionCompat {
            if(windowIndex<mediaSource.audioMediMetaData.size){
                return mediaSource.audioMediMetaData[windowIndex].description
            }
            return MediaDescriptionCompat.Builder().build()
        }
    }
    inner class PlayerNotificationListener : PlayerNotificationManager.NotificationListener{
        override fun onNotificationCancelled(notificationId: Int, dismissedByUser: Boolean) {
            stopForeground(true)
            isForegroundService = false
            stopSelf()
        }

        @SuppressLint("ForegroundServiceType")
        override fun onNotificationPosted(
            notificationId: Int,
            notification: Notification,
            ongoing: Boolean
        ) {
            if (ongoing && !isForegroundService){
                ContextCompat.startForegroundService(applicationContext, Intent(applicationContext,this@MediaPlayerService.javaClass))
                startForeground(notificationId,notification)
                isForegroundService = true
            }
        }
    }
    inner class AudioMediaPlayBackPrepare:MediaSessionConnector.PlaybackPreparer{
        override fun onCommand(
            player: Player,
            command: String,
            extras: Bundle?,
            cb: ResultReceiver?
        ): Boolean {
            return false
        }

        override fun getSupportedPrepareActions(): Long = PlaybackStateCompat.ACTION_PREPARE_FROM_MEDIA_ID or PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID

        override fun onPrepare(playWhenReady: Boolean) = Unit

        override fun onPrepareFromMediaId(
            mediaId: String,
            playWhenReady: Boolean,
            extras: Bundle?
        ) {

            mediaSource.whenReady {
                val itemToPlay = mediaSource.audioMediMetaData.find {
                    it.description.mediaId == mediaId
                }

                currentPlayingMedia = itemToPlay
                preparePlayer(mediaMetaData = mediaSource.audioMediMetaData,
                                itemToPlay= itemToPlay,
                                playWhenReady= playWhenReady)
            }

        }

        override fun onPrepareFromSearch(query: String, playWhenReady: Boolean, extras: Bundle?) = Unit

        override fun onPrepareFromUri(uri: Uri, playWhenReady: Boolean, extras: Bundle?) = Unit

        private fun preparePlayer(mediaMetaData:List<MediaMetadataCompat>,itemToPlay:MediaMetadataCompat?,playWhenReady:Boolean){
            val indexToPlay = if (currentPlayingMedia == null) 0
            else mediaMetaData.indexOf(itemToPlay)
            exoPlayer.addListener(PlayerEventListener())
            exoPlayer.setMediaSource(mediaSource.asMediaSource(dataSourceFactory))
            exoPlayer.prepare()
            exoPlayer.seekTo(indexToPlay,0)
            exoPlayer.playWhenReady = playWhenReady
        }
    }

    private inner class PlayerEventListener:Player.Listener{
        override fun onPlaybackStateChanged(playbackState: Int) {
            when(playbackState){
                Player.STATE_BUFFERING,
                    Player.STATE_READY ->{
                        mediaPlayerNotificationManager.showNotification(exoPlayer)
                    }
                else -> {
                    mediaPlayerNotificationManager.hideNofitication()
                }
            }
        }

        override fun onEvents(player: Player, events: Player.Events) {
            super.onEvents(player, events)
            currentDuration = player.duration
        }

        override fun onPlayerError(error: PlaybackException) {
            super.onPlayerError(error)
        }
    }
}