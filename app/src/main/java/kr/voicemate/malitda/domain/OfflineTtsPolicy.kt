package kr.voicemate.malitda.domain

/** Previews must not silently send a private message to a network voice service. */
object OfflineTtsPolicy {
    fun permits(language: String?, networkRequired: Boolean?, installed: Boolean, readyOffline: Boolean = true): Boolean =
        readyOffline && language == "ko" && networkRequired == false && installed
}
