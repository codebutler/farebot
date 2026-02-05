package com.codebutler.farebot.shared.platform

interface Analytics {
    fun logEvent(name: String, params: Map<String, String> = emptyMap())
}

class NoOpAnalytics : Analytics {
    override fun logEvent(name: String, params: Map<String, String>) {
        // No-op: wire to a real analytics provider as needed
    }
}
