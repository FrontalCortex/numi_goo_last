package com.example.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment

class QuestionMediaPickerDialogFragment : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT
        )
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        return inflater.inflate(R.layout.dialog_question_media_picker, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Dışarı tıkla kapansın
        view.setOnClickListener { dismissAllowingStateLoss() }

        view.findViewById<View>(R.id.card).setOnClickListener { /* consume */ }

        view.findViewById<View>(R.id.btnCamera).setOnClickListener {
            parentFragmentManager.setFragmentResult(
                REQUEST_KEY,
                bundleOf(RESULT_PICK to PICK_CAMERA)
            )
            dismissAllowingStateLoss()
        }
        view.findViewById<View>(R.id.btnVideo).setOnClickListener {
            parentFragmentManager.setFragmentResult(
                REQUEST_KEY,
                bundleOf(RESULT_PICK to PICK_VIDEO)
            )
            dismissAllowingStateLoss()
        }
    }

    companion object {
        const val TAG = "QuestionMediaPickerDialog"
        const val REQUEST_KEY = "question_media_picker_request"
        const val RESULT_PICK = "result_pick"
        const val PICK_CAMERA = "camera"
        const val PICK_VIDEO = "video"
    }
}

