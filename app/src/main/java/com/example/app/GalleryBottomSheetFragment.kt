package com.example.app

import android.app.Dialog
import android.content.ContentUris
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

data class GalleryMediaItem(val uri: Uri, val type: String, val dateAdded: Long) {
    companion object {
        const val TYPE_IMAGE = "image"
        const val TYPE_VIDEO = "video"
    }
}

class GalleryBottomSheetFragment : BottomSheetDialogFragment() {

    var onMediaSelected: ((Uri, String) -> Unit)? = null
    private var selectedUri: Uri? = null
    private var selectedType: String? = null
    private var adapter: GalleryGridAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.bottom_sheet_gallery, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBottomSheetBehavior()
        view.findViewById<View>(R.id.gallerySheetClose).setOnClickListener { dismiss() }
        val grid = view.findViewById<RecyclerView>(R.id.galleryGrid)
        grid.layoutManager = GridLayoutManager(requireContext(), 3)
        val items = loadGalleryMedia()
        adapter = GalleryGridAdapter(items,
            onItemClick = { item ->
                if (selectedUri == item.uri) {
                    selectedUri = null
                    selectedType = null
                    adapter?.setSelectedUri(null)
                    return@GalleryGridAdapter
                }
                selectedUri = item.uri
                selectedType = item.type
                adapter?.setSelectedUri(item.uri)
                onMediaSelected?.invoke(item.uri, item.type)
                dismiss()
            },
            onItemLongClick = { item -> showPreview(item) }
        )
        grid.adapter = adapter
    }

    private var previewDialog: Dialog? = null
    private var previewPlayer: ExoPlayer? = null

    private fun showPreview(item: GalleryMediaItem) {
        if (item.type == GalleryMediaItem.TYPE_IMAGE) {
            val dialog = Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen).apply {
                setContentView(R.layout.dialog_image)
                window?.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            }
            val imageView = dialog.findViewById<ImageView>(R.id.dialogImageView)
            Glide.with(this).load(item.uri).into(imageView)
            dialog.findViewById<View>(R.id.dialogImageClose).setOnClickListener { dialog.dismiss() }
            dialog.show()
        } else {
            previewDialog?.dismiss()
            previewPlayer?.release()
            val dialog = Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen).apply {
                setContentView(R.layout.dialog_video_player)
                window?.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            }
            val playerView = dialog.findViewById<PlayerView>(R.id.dialogPlayerView)
            val player = ExoPlayer.Builder(requireContext()).build().also { previewPlayer = it }
            playerView.player = player
            player.setMediaItem(MediaItem.fromUri(item.uri))
            player.prepare()
            player.playWhenReady = true
            dialog.findViewById<View>(R.id.dialogVideoClose).setOnClickListener {
                previewPlayer?.release()
                previewPlayer = null
                dialog.dismiss()
            }
            dialog.setOnDismissListener {
                previewPlayer?.release()
                previewPlayer = null
                playerView.player = null
            }
            dialog.show()
            previewDialog = dialog
        }
    }

    private fun setupBottomSheetBehavior() {
        dialog?.setOnShowListener { dialogInterface ->
            val bottomSheet = (dialogInterface as? android.app.Dialog)?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                behavior.isFitToContents = true
                val peekHeightPx = (320 * resources.displayMetrics.density).toInt()
                behavior.peekHeight = peekHeightPx
                behavior.state = BottomSheetBehavior.STATE_COLLAPSED
            }
        }
    }

    private fun loadGalleryMedia(): List<GalleryMediaItem> {
        val list = mutableListOf<GalleryMediaItem>()
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DATE_ADDED
        )
        try {
            requireContext().contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                "${MediaStore.MediaColumns.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val date = cursor.getLong(dateCol)
                    val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                    list.add(GalleryMediaItem(uri, GalleryMediaItem.TYPE_IMAGE, date))
                }
            }
            requireContext().contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                "${MediaStore.MediaColumns.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val date = cursor.getLong(dateCol)
                    val uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                    list.add(GalleryMediaItem(uri, GalleryMediaItem.TYPE_VIDEO, date))
                }
            }
            list.sortByDescending { it.dateAdded }
        } catch (e: SecurityException) {
            Toast.makeText(requireContext(), "Galeriye erişim izni gerekli.", Toast.LENGTH_SHORT).show()
        }
        return list
    }

    companion object {
        fun newInstance() = GalleryBottomSheetFragment()
    }
}

private class GalleryGridAdapter(
    private val items: List<GalleryMediaItem>,
    private val onItemClick: (GalleryMediaItem) -> Unit,
    private val onItemLongClick: (GalleryMediaItem) -> Unit
) : RecyclerView.Adapter<GalleryGridAdapter.VH>() {

    private var selectedUri: Uri? = null

    fun setSelectedUri(uri: Uri?) {
        selectedUri = uri
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_gallery_media, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position], selectedUri == items[position].uri, onItemClick, onItemLongClick)
    }

    override fun getItemCount() = items.size

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val thumb = itemView.findViewById<ImageView>(R.id.thumb)
        private val videoDuration = itemView.findViewById<TextView>(R.id.videoDuration)
        private val selectionOverlay = itemView.findViewById<View>(R.id.selectionOverlay)

        fun bind(item: GalleryMediaItem, selected: Boolean, onItemClick: (GalleryMediaItem) -> Unit, onItemLongClick: (GalleryMediaItem) -> Unit) {
            Glide.with(itemView.context).load(item.uri).centerCrop().into(thumb)
            videoDuration.visibility = if (item.type == GalleryMediaItem.TYPE_VIDEO) View.VISIBLE else View.GONE
            if (item.type == GalleryMediaItem.TYPE_VIDEO) {
                videoDuration.text = ""
            }
            selectionOverlay.visibility = if (selected) View.VISIBLE else View.GONE
            itemView.setOnClickListener { onItemClick(item) }
            itemView.setOnLongClickListener { onItemLongClick(item); true }
        }
    }
}

