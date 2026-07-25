package com.example.app

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.bottomnavigation.BottomNavigationView

class CustomBottomNavigationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = com.google.android.material.R.attr.bottomNavigationStyle,
) : BottomNavigationView(context, attrs, defStyleAttr) {

    override fun getMaxItemCount(): Int = 7
}
