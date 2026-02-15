package com.codebutler.farebot.base.mdst

actual object ResourceAccessor {
    actual fun openMdstFile(dbName: String): ByteArray? =
        throw UnsupportedOperationException("ResourceAccessor is not yet available on wasmJs")
}
