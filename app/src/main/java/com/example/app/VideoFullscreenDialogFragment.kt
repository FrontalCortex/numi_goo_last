package com.example.app

import android.content.DialogInterface
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageButton
import androidx.fragment.app.DialogFragment
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer

class VideoFullscreenDialogFragment : DialogFragment() {

    private var exoPlayer: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.dialog_video_fullscreen, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val videoUrl = arguments?.getString(ARG_VIDEO_URL) ?: return
        exoPlayer = ExoPlayer.Builder(requireContext()).build().also { player ->
            view.findViewById<androidx.media3.ui.PlayerView>(R.id.fullscreenPlayerView).player = player
            player.setMediaItem(MediaItem.fromUri(Uri.parse(videoUrl)))
            player.prepare()
            player.playWhenReady = true
        }
        view.findViewById<ImageButton>(R.id.fullscreenCloseButton).setOnClickListener { dismiss() }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        arguments?.getString(ARG_RESULT_REQUEST_KEY)?.let { key ->
            parentFragmentManager.setFragmentResult(key, Bundle())
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
            setBackgroundDrawableResource(android.R.color.black)
            addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            decorView?.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        }
    }

    override fun onDestroyView() {
        exoPlayer?.release()
        exoPlayer = null
        view?.findViewById<androidx.media3.ui.PlayerView>(R.id.fullscreenPlayerView)?.player = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_VIDEO_URL = "video_url"
        private const val ARG_RESULT_REQUEST_KEY = "result_request_key"

        /**
         * @param videoUrl Video URI (file or http).
         * @param resultRequestKey Optional: when dialog is dismissed, setFragmentResult(key, Bundle()) is called so caller can resume its player.
         */
        fun newInstance(videoUrl: String, resultRequestKey: String? = null): VideoFullscreenDialogFragment {
            return VideoFullscreenDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_VIDEO_URL, videoUrl)
                    resultRequestKey?.let { putString(ARG_RESULT_REQUEST_KEY, it) }
                }
            }
        }
    }
}
