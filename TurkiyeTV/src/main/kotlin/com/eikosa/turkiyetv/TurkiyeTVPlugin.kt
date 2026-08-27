package com.eikosa.turkiyetv

import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class TurkiyeTVPlugin : Plugin() {
    override fun load(context: Context) {
        registerMainAPI(TurkiyeTVProvider())
    }
}
