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
        val appId = context.packageName
        val appVersion = try {
            context.packageManager.getPackageInfo(appId, 0).versionName
        } catch (e: Exception) {
            null
        }
        current = make(BuildConfig.SDK_VERSION, Build.VERSION.RELEASE, Build.MODEL, appId, appVersion)
    }

    fun make(sdkVersion: String?, osVersion: String?, model: String?, appId: String?, appVersion: String?): String {
        val device = StringBuilder("android; Android ").append(token(osVersion).ifEmpty { UNKNOWN })
        text(model).takeIf { it.isNotEmpty() }?.let { device.append("; ").append(it) }

        val header = StringBuilder("SegmentifySDK/").append(token(sdkVersion).ifEmpty { UNKNOWN })
            .append(" (").append(device).append(")")

        token(appId).takeIf { it.isNotEmpty() }?.let { id ->
            header.append(' ').append(id)
            token(appVersion).takeIf { it.isNotEmpty() }?.let { header.append('/').append(it) }
        }
        return header.toString()
    }

    private const val UNKNOWN = "unknown"

    /** Printable ASCII without the characters that delimit the parts of the header. */
    private fun text(value: String?): String =
        value.orEmpty().filter { it.code in 0x20..0x7E && it !in ";()" }.trim()

    /** [text] without spaces and slashes, for the parts that are a single token. */
    private fun token(value: String?): String = text(value).filter { it != ' ' && it != '/' }
}
