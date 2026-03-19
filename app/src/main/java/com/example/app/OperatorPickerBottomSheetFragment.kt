package com.example.app

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.NumberPicker
import androidx.core.os.bundleOf
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class OperatorPickerBottomSheetFragment : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.bottom_sheet_operator_picker, container, false)

        val picker = view.findViewById<NumberPicker>(R.id.operatorPicker)
        val okButton = view.findViewById<Button>(R.id.okButton)
        val cancelButton = view.findViewById<Button>(R.id.cancelButton)

        val ops = arrayOf("+", "-", "x")
        picker.minValue = 0
        picker.maxValue = ops.lastIndex
        picker.displayedValues = ops
        picker.wrapSelectorWheel = true

        val initial = arguments?.getString(ARG_INITIAL_OP) ?: "+"
        picker.value = ops.indexOf(initial).takeIf { it >= 0 } ?: 0

        okButton.setOnClickListener {
            parentFragmentManager.setFragmentResult(
                REQUEST_KEY,
                bundleOf(BUNDLE_SELECTED_OP to ops[picker.value])
            )
            dismissAllowingStateLoss()
        }

        cancelButton.setOnClickListener { dismissAllowingStateLoss() }

        return view
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // keep default BottomSheetDialog behavior
        return super.onCreateDialog(savedInstanceState)
    }

    companion object {
        const val TAG = "OperatorPickerBottomSheet"
        const val REQUEST_KEY = "operator_picker_request"
        const val BUNDLE_SELECTED_OP = "selected_operator"
        private const val ARG_INITIAL_OP = "initial_operator"

        fun newInstance(initialOp: String): OperatorPickerBottomSheetFragment {
            return OperatorPickerBottomSheetFragment().apply {
                arguments = bundleOf(ARG_INITIAL_OP to initialOp)
            }
        }
    }
}

