package de.muellerml.handball4all.util

import com.fasterxml.jackson.databind.JsonMappingException

suspend fun <T> detectBreakingApiChanges(lambda: suspend () -> T) : T {
    return runCatching { lambda() }.getOrElse {
        when(it) {
            is JsonMappingException -> throw PossibleApiBreakException(it)
            else -> throw it
        }
    }
}

class PossibleApiBreakException(e: Throwable) : RuntimeException(e)
