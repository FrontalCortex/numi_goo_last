package com.example.app.model

import com.example.app.BunnyMascotView

/** Haritadaki rehber panelinin ([com.example.app.GuidePanelView]) bir sayfası: Sobi'nin hâli ve yazı. */
data class GuidePanelData(
    val emote: BunnyMascotView.Emote,
    val text: String
)
