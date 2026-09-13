package com.example.aether.analysis

class AnalysisException(
    message: String,
    val unrecoverable: Boolean = true,
    cause: Throwable? = null
) : Exception(message, cause)
