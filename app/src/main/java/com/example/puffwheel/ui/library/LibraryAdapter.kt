package com.example.puffwheel.ui.library

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
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
        fun bind(track: Track) {
            binding.textTitle.text = track.title
            binding.textArtist.text = track.artist
            binding.root.setOnClickListener {
                onClick(track)
            }
        }
    }

    class TrackDiffCallback : DiffUtil.ItemCallback<Track>() {
        override fun areItemsTheSame(oldItem: Track, newItem: Track): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Track, newItem: Track): Boolean = oldItem == newItem
    }
}
