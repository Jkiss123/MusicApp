package com.example.mediaplayer.media.service

import android.media.browse.MediaBrowser
import android.os.Bundle
import android.service.media.MediaBrowserService
import com.example.mediaplayer.media.constants.AudioContanst
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MediaPlayerService : MediaBrowserService() {
    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot {
        return BrowserRoot(AudioContanst.MEDIA_ROOT_ID,null)
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowser.MediaItem>>
    ) {
        TODO("Not yet implemented")
    }
}