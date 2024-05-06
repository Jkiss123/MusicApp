package com.example.mediaplayer.media.service

import android.media.browse.MediaBrowser
import android.os.Bundle
import android.service.media.MediaBrowserService
import android.support.v4.media.MediaBrowserCompat
import androidx.media.MediaBrowserServiceCompat
import com.example.mediaplayer.media.constants.AudioContanst
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.source.hls.DefaultHlsDataSourceFactory
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MediaPlayerService : MediaBrowserServiceCompat() {

    @Inject
    lateinit var dataSourceFactory: CacheDataSource.Factory
    @Inject
    lateinit var exoPlayer:ExoPlayer

    @Inject
    lateinit var mediaSource : MediaSource

    @Inject
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
        TODO("Not yet implemented")
    }
}