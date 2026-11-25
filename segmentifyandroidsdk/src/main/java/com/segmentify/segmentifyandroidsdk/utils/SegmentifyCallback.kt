package com.segmentify.segmentifyandroidsdk.utils

interface SegmentifyCallback<T> {
    fun onDataLoaded(data: T)

    /**
     * Called instead of [onDataLoaded] when the request could not be completed:
     * no network, an error answer from the server, or the event was given up
     * before it could be sent. [SegmentifyException.httpStatus] and
     * [Throwable.cause] say which.
     *
     * Has an empty default implementation, so existing callbacks keep compiling
     * and keep working; override it to stop a loading state or to retry.
     */
    fun onError(error: SegmentifyException) {
    }
}
