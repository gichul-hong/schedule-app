package com.scheduleapp.core.platform

import android.content.Context

internal object PlatformContextHolder {
    var appContext: Context? = null
}

fun initPlatformContext(context: Context) {
    PlatformContextHolder.appContext = context.applicationContext
}
