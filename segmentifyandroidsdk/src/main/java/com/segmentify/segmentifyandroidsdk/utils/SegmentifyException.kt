package com.segmentify.segmentifyandroidsdk.utils

/**
 * Why a request to Segmentify did not produce a result.
 *
 * @property httpStatus the HTTP status when the server answered, null when
 *   there was no answer (no network, timeout) or the event was never sent
 * @property statusCode the backend's own reason when it rejected the event,
 *   for example NO_SESSIONID or INVALID_EVENT; null otherwise
 */
class SegmentifyException @JvmOverloads constructor(
    message: String,
    val httpStatus: Int? = null,
    cause: Throwable? = null,
    val statusCode: String? = null
) : Exception(message, cause)
