package kr.voicemate.malitda.ui.v2

/** Navigation only: role choice does not grant data-processing or supporter permission. */
object OnboardingPolicy {
    fun requiresConsent(needsConsent: Boolean, route: String): Boolean =
        needsConsent && route !in setOf("U00", "U12DATA")

    fun destination(prepareWithSupporter: Boolean, consentGranted: Boolean): String =
        if (!consentGranted) "U00C" else if (prepareWithSupporter) "U07" else "U01"
}
