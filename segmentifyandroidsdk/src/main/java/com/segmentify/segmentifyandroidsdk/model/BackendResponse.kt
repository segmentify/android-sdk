package com.segmentify.segmentifyandroidsdk.model

/**
 * What every answer of the event endpoint carries. The backend answers with
 * HTTP 200 even when it rejects an event; [statusCode] is then something other
 * than "SUCCESS" (for example NO_SESSIONID, INVALID_EVENT, BOT, TIMEOUT).
 */
interface BackendResponse {
    val statusCode: String?
}
