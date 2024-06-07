package com.example.mediaplayer.ui.audio

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.mediaplayer.data.model.Audio
import com.example.mediaplayer.databinding.ItemLayoutSongBinding

class ListSongAdapter(): RecyclerView.Adapter<ListSongAdapter.ListSongViewHolder>() {

    inner class ListSongViewHolder(private val binding:ItemLayoutSongBinding) : RecyclerView.ViewHolder(binding.root){
        fun bind( music: Audio){
            binding.apply {
                textViewTitle.text = music.titile
                textViewDuration.text = music.duration.toString()
                textViewContent.text = music.artist
            }
        }
    }
    val differCallback = object : DiffUtil.ItemCallback<Audio>(){
        override fun areItemsTheSame(oldItem: Audio, newItem: Audio): Boolean {
            return  oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Audio, newItem: Audio): Boolean {
            return  oldItem == newItem
        }
    }

    val differ = AsyncListDiffer(this,differCallback)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListSongViewHolder {
        return ListSongViewHolder(ItemLayoutSongBinding.inflate(LayoutInflater.from(parent.context),parent,false))
    }

    override fun onBindViewHolder(holder: ListSongViewHolder, position: Int) {
        val song =differ.currentList[position]
        holder.bind(song)
        holder.itemView.setOnClickListener {
            onClick?.invoke(song)
        }
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    var onClick : ((Audio) ->Unit)? = null
}