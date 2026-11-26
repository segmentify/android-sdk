package com.segmentify.segmentifyandroidsdk.utils

import android.content.Context
import android.os.Build
import com.segmentify.segmentifyandroidsdk.BuildConfig

/**
 * The `User-Agent` header of every request the SDK sends:
 *
 *     SegmentifySDK/<sdkVersion> (android; Android <osVersion>; <model>) <appId>/<appVersion>
 *
 * The backend reads the device and operating system from it (SFY-28912), so the
 * format is a contract shared with the iOS and React Native SDKs.
 */
object SegmentifyUserAgent {
    const val HEADER_NAME = "User-Agent"

    /** The header for this device; [init] adds the host app. */
    @Volatile
    var current: String = make(BuildConfig.SDK_VERSION, Build.VERSION.RELEASE, Build.MODEL, null, null)
        private set

    /** Adds the host app's id and version to [current]. */
    fun init(context: Context) {
    }

    fun make(sdkVersion: String?, osVersion: String?, model: String?, appId: String?, appVersion: String?): String {
        return ""
    }
}
