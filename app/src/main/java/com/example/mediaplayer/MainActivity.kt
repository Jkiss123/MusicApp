package com.example.mediaplayer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import com.example.mediaplayer.ui.audio.AudioViewModel
import dagger.hilt.android.AndroidEntryPoint
import androidx.activity.viewModels
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mediaplayer.data.model.Audio
import com.example.mediaplayer.databinding.ActivityMainBinding
import com.example.mediaplayer.ui.audio.ListSongAdapter

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel : AudioViewModel by viewModels()
    private lateinit var audioList : List<Audio>
    private val adapterL:ListSongAdapter by lazy { ListSongAdapter() }
    private lateinit var binding: ActivityMainBinding
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        requestPermission()
        setupListSong()

    }






















fun setupListSong(){
    audioList = viewModel.audioLists
    adapterL.differ.submitList(audioList)
    binding.recvListSong.apply {
        layoutManager = LinearLayoutManager(context,  LinearLayoutManager.VERTICAL,false)
        adapter = adapterL
    }
}
//Request Permisstion
    private  val multiplePermissionId = 14
    private val mutiplePremissionName = if (Build.VERSION.SDK_INT>=33){
        arrayListOf(
            android.Manifest.permission.READ_MEDIA_AUDIO,
            android.Manifest.permission.READ_MEDIA_VIDEO,
            android.Manifest.permission.READ_MEDIA_IMAGES
        )
    }else{
        arrayListOf(
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }


    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun requestPermission() {
        if (!PermissionAsker.checkExternalStoragePermission(this) || !PermissionAsker.checkAudioStorePermission(this)) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.READ_MEDIA_AUDIO),
                MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE,
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private const val MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1101
    }
}