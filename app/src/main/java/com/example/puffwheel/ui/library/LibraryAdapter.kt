package com.example.puffwheel.ui.library

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.example.puffwheel.data.db.Track
import com.example.puffwheel.databinding.ItemTrackBinding

class LibraryAdapter(
    private val onTrackClicked: (Track) -> Unit
) : ListAdapter<Track, LibraryAdapter.TrackViewHolder>(TrackDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackViewHolder {
        val binding = ItemTrackBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TrackViewHolder(binding, onTrackClicked)
    }

    override fun onBindViewHolder(holder: TrackViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class TrackViewHolder(
        private val binding: ItemTrackBinding,
        private val onClick: (Track) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        
        private var currentTrack: Track? = null
        
        fun bind(track: Track) {
            currentTrack = track
            binding.textTitle.text = track.title
            binding.textArtist.text = track.artist
            binding.imageArtwork.setImageBitmap(null)
            binding.root.setOnClickListener {
                onClick(track)
            }
            
            val albumId = track.artworkKey?.toLongOrNull()
            if (albumId != null) {
                // In a real app we would use Glide/Coil or a dedicated memory cache
                // For Phase 4 we demonstrate the loader API
                (binding.root.context as? androidx.lifecycle.LifecycleOwner)?.lifecycleScope?.launch {
                    val bitmap = com.example.puffwheel.util.ArtworkLoader.loadArtwork(binding.root.context, albumId)
                    if (currentTrack?.id == track.id) {
                        binding.imageArtwork.setImageBitmap(bitmap)
                    }
                }
            }
        }
    }

    class TrackDiffCallback : DiffUtil.ItemCallback<Track>() {
        override fun areItemsTheSame(oldItem: Track, newItem: Track): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Track, newItem: Track): Boolean = oldItem == newItem
    }
}
