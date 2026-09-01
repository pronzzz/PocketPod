package com.pocketpod.app.ui.nowplaying

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.pocketpod.app.databinding.FragmentNowPlayingBinding
import com.pocketpod.app.ui.MainActivity

class NowPlayingFragment : Fragment() {

    private var _binding: FragmentNowPlayingBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNowPlayingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // This is a placeholder UI binding. 
        // In a complete implementation, this connects to PlaybackViewModel 
        // and observes the MediaController's current position and state.
        
        binding.btnPlayPause.setOnClickListener {
            val controller = (activity as? MainActivity)?.mediaController
            if (controller?.isPlaying == true) {
                controller.pause()
            } else {
                controller?.play()
            }
        }
        
        binding.btnNext.setOnClickListener {
            (activity as? MainActivity)?.mediaController?.seekToNextMediaItem()
        }
        
        binding.btnPrev.setOnClickListener {
            (activity as? MainActivity)?.mediaController?.seekToPreviousMediaItem()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
