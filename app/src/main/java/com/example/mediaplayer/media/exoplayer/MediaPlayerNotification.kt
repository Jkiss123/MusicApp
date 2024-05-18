package com.example.mediaplayer.media.exoplayer

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import com.example.mediaplayer.R
import com.example.mediaplayer.media.constants.AudioContanst
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerNotificationManager

internal class MediaPlayerNotification (
    context:Context,
    sessionToken:MediaSessionCompat.Token,
    notificationListener : PlayerNotificationManager.NotificationListener
){
    private val notificationManager:PlayerNotificationManager

    init {
        val mediaController = MediaControllerCompat(context,sessionToken)
        val builder= PlayerNotificationManager.Builder(
            context,
            AudioContanst.PLAYBACK_NOTIFICATION_ID,
            AudioContanst.PLAYBACK_NOTIFICATION_CHANNEL_ID,

        )
        with(builder){
            setMediaDescriptionAdapter(DescriptionAdapter(mediaController))
            setNotificationListener(notificationListener)
            setChannelNameResourceId(1)
            setChannelDescriptionResourceId(2)

        }
        notificationManager = builder.build()
        with(notificationManager){
            setMediaSessionToken(sessionToken)
            setSmallIcon(R.drawable.ic_music_circle)
            setUseRewindAction(false)
            setUseFastForwardAction(false)
        }

    }
    fun hideNofitication(){
        notificationManager.setPlayer(null)
    }
    fun showNotification(player:Player){
        notificationManager.setPlayer(player)
    }
    inner class  DescriptionAdapter(private val controller:MediaControllerCompat) : PlayerNotificationManager.MediaDescriptionAdapter{
        override fun getCurrentContentTitle(player: Player): CharSequence = controller.metadata.description.title.toString()

        override fun createCurrentContentIntent(player: Player): PendingIntent?= controller.sessionActivity

        override fun getCurrentContentText(player: Player): CharSequence? =controller.metadata.description.subtitle

        override fun getCurrentLargeIcon(
            player: Player,
            callback: PlayerNotificationManager.BitmapCallback
        ): Bitmap? {
            return null
        }
    }

}