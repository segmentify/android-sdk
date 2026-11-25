package com.segmentify.segmentifyandroidsdk.utils

/**
 * Why a request to Segmentify did not produce a result.
 *
 * @property httpStatus the status code when the server answered with an error,
 *   null when there was no answer (no network, timeout) or the event was never sent
 */
class SegmentifyException(
    message: String,
    val httpStatus: Int? = null,
    cause: Throwable? = null
) : Exception(message, cause)
