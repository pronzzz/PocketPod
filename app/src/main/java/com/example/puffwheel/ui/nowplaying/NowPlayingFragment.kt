package com.example.puffwheel.ui.nowplaying

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.example.puffwheel.databinding.FragmentNowPlayingBinding
import com.example.puffwheel.ui.MainActivity
import com.example.puffwheel.util.ArtworkLoader
import kotlinx.coroutines.launch

class NowPlayingFragment : Fragment() {
    private var _binding: FragmentNowPlayingBinding? = null
    private val binding get() = _binding!!
    
    private val playerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            updateUI()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updatePlayPauseButton(isPlaying)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNowPlayingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val controller = (requireActivity() as MainActivity).mediaController
        controller?.addListener(playerListener)
        
        binding.btnPlayPause.setOnClickListener {
            val ctrl = (requireActivity() as MainActivity).mediaController
            if (ctrl?.isPlaying == true) {
                ctrl.pause()
            } else {
                ctrl?.play()
            }
        }
        
        binding.btnNext.setOnClickListener {
            (requireActivity() as MainActivity).mediaController?.seekToNext()
        }
        
        binding.btnPrev.setOnClickListener {
            (requireActivity() as MainActivity).mediaController?.seekToPrevious()
        }
        
        updateUI()
    }
    
    private fun updateUI() {
        val controller = (requireActivity() as? MainActivity)?.mediaController
        if (controller == null) return
        
        val currentMediaItem = controller.currentMediaItem
        if (currentMediaItem != null) {
            binding.textNpTitle.text = currentMediaItem.mediaMetadata.title ?: "Unknown Title"
            binding.textNpArtist.text = currentMediaItem.mediaMetadata.artist ?: "Unknown Artist"
            
            updatePlayPauseButton(controller.isPlaying)
            
            // For artwork, we can parse the ID from the URI or metadata
            // In a full implementation we'd inject this via MediaMetadata.Builder
            val uriStr = currentMediaItem.localConfiguration?.uri?.toString() ?: ""
            val id = uriStr.substringAfterLast("/").toLongOrNull()
            
            if (id != null) {
                // Since MediaStore uses the audio ID for the thumbnail in our loader or album ID
                // Ideally we pass albumId to mediaMetadata, but this is a placeholder approach.
                // We'll skip artwork loading if we don't have the explicit album ID
                // For a robust implementation we should put albumId in MediaItem extras
            }
        } else {
            binding.textNpTitle.text = "No track selected"
            binding.textNpArtist.text = ""
        }
    }
    
    private fun updatePlayPauseButton(isPlaying: Boolean) {
        if (isPlaying) {
            binding.btnPlayPause.setImageResource(android.R.drawable.ic_media_pause)
        } else {
            binding.btnPlayPause.setImageResource(android.R.drawable.ic_media_play)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (requireActivity() as? MainActivity)?.mediaController?.removeListener(playerListener)
        _binding = null
    }
}
