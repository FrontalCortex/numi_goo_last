package com.example.app

import android.content.DialogInterface
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import java.io.File
import java.io.FileOutputStream

class ImageAnnotateDialogFragment : DialogFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.dialog_image_annotate, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val imagePath = arguments?.getString(ARG_IMAGE_PATH).orEmpty()
        if (imagePath.isBlank() || !File(imagePath).exists()) {
            Toast.makeText(requireContext(), "Resim bulunamadı.", Toast.LENGTH_SHORT).show()
            dismiss()
            return
        }

        val content = view.findViewById<View>(R.id.annotateContent)
        val imageView = view.findViewById<ImageView>(R.id.annotateImage)
        val drawingOverlay = view.findViewById<DrawingOverlayView>(R.id.annotateDrawingOverlay)
        val doneButton = view.findViewById<ImageButton>(R.id.annotateDoneButton)

        // Mevcut view_drawing_controls_overlay'i kullan (çekmece davranışı korunur).
        val drawerRoot = view.findViewById<View>(R.id.drawingControlsRoot)
        val panel = view.findViewById<View>(R.id.drawingControlsContainer)
        val drawerButton = view.findViewById<View>(R.id.drawerButton)
        val drawerButtonIcon = view.findViewById<android.widget.ImageView>(R.id.drawerButtonIcon)

        val pencilButton = view.findViewById<ImageButton>(R.id.pencilButton)
        val colorStrip = view.findViewById<DrawingColorStripView>(R.id.colorStrip)
        val undoButton = view.findViewById<ImageButton>(R.id.undoButton)
        val strokeWidthSlider = view.findViewById<VerticalSliderView>(R.id.strokeWidthSeekBar)
        val strokePreview = view.findViewById<StrokePreviewView>(R.id.strokePreview)

        // Image yükle
        imageView.setImageURI(Uri.fromFile(File(imagePath)))

        // Çizim başlangıçta açık
        drawingOverlay.setDrawingEnabled(true)
        drawingOverlay.visibility = View.VISIBLE

        // Sol tarafta çekmece: kapalıyken panel solda gizli kalsın, sadece handle görünsün.
        var isOpen = false
        drawerRoot.post {
            isOpen = false
            drawerRoot.translationX = -panel.width.toFloat()
            drawerButtonIcon.rotationY = -180f
        }
        drawerButtonIcon.cameraDistance = 8000f * resources.displayMetrics.density
        drawerButton.setOnClickListener {
            val targetOpen = !isOpen
            isOpen = targetOpen
            drawerButtonIcon.animate()
                .rotationY(if (targetOpen) 0f else -180f)
                .setDuration(200)
                .start()
            drawerRoot.animate()
                .translationX(if (targetOpen) 0f else -panel.width.toFloat())
                .setDuration(200)
                .start()
        }

        // Pencil toggle
        pencilButton.isSelected = true
        pencilButton.setBackgroundResource(R.drawable.bg_pencil_circle_selected)
        pencilButton.setOnClickListener {
            val enabled = !pencilButton.isSelected
            pencilButton.isSelected = enabled
            if (enabled) {
                pencilButton.setBackgroundResource(R.drawable.bg_pencil_circle_selected)
                drawingOverlay.setDrawingEnabled(true)
            } else {
                pencilButton.setBackgroundResource(R.drawable.bg_pencil_circle)
                drawingOverlay.setDrawingEnabled(false)
            }
        }

        // Renk seçimi
        colorStrip.listener = object : DrawingColorStripView.OnColorSelectedListener {
            override fun onColorSelected(color: Int) {
                drawingOverlay.setStrokeColor(color)
                pencilButton.imageTintList = android.content.res.ColorStateList.valueOf(color)
                strokePreview.setColor(color)
            }
        }

        // Kalınlık
        strokeWidthSlider.max = 1000
        strokeWidthSlider.progress = 500
        strokeWidthSlider.listener = object : VerticalSliderView.Listener {
            override fun onStartTrackingTouch() {
                pencilButton.visibility = View.INVISIBLE
                strokePreview.visibility = View.VISIBLE
            }

            override fun onProgressChanged(progress: Int, fromUser: Boolean) {
                val fraction = progress / strokeWidthSlider.max.toFloat()
                val width = DrawingOverlayView.MIN_STROKE_WIDTH +
                    fraction * (DrawingOverlayView.MAX_STROKE_WIDTH - DrawingOverlayView.MIN_STROKE_WIDTH)
                drawingOverlay.setStrokeWidth(width)
                strokePreview.setStrokeWidth(width)
            }

            override fun onStopTrackingTouch() {
                pencilButton.visibility = View.VISIBLE
                strokePreview.visibility = View.GONE
            }
        }

        // Undo
        undoButton.setOnClickListener { drawingOverlay.undoLastStroke() }
        undoButton.setOnLongClickListener {
            drawingOverlay.clearAllStrokes()
            true
        }

        // Kaydet + çık
        doneButton.setOnClickListener {
            // Kontrolleri kayda dahil etme
            drawerRoot.visibility = View.GONE
            doneButton.visibility = View.GONE

            val out = Bitmap.createBitmap(content.width, content.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(out)
            content.draw(canvas)
            runCatching {
                FileOutputStream(File(imagePath)).use { fos ->
                    out.compress(Bitmap.CompressFormat.JPEG, 95, fos)
                }
            }.onFailure {
                Toast.makeText(requireContext(), "Kaydedilemedi.", Toast.LENGTH_SHORT).show()
                // geri göster
                drawerRoot.visibility = View.VISIBLE
                doneButton.visibility = View.VISIBLE
                return@setOnClickListener
            }
            out.recycle()
            parentFragmentManager.setFragmentResult(RESULT_KEY, Bundle())
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
            setBackgroundDrawableResource(android.R.color.black)
            addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)

            // Immersive
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                setDecorFitsSystemWindows(false)
                insetsController?.let { controller ->
                    controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                    controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            } else {
                @Suppress("DEPRECATION")
                decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
            }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
    }

    companion object {
        private const val ARG_IMAGE_PATH = "image_path"
        const val RESULT_KEY = "ImageAnnotateDialogFragment_result"

        fun newInstance(imagePath: String): ImageAnnotateDialogFragment {
            return ImageAnnotateDialogFragment().apply {
                arguments = Bundle().apply { putString(ARG_IMAGE_PATH, imagePath) }
            }
        }
    }
}

