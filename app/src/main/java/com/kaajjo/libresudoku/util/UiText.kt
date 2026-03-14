package com.kaajjo.libresudoku.util

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

/**
 * A sealed class that abstracts string handling, allowing ViewModels to provide
 * UI text without needing a [Context] reference.
 */
sealed interface UiText {

    /**
     * Represents a string resource defined in resources
     *
     * @property resId The unique resource identifier.
     * @property args Optional arguments for string formatting.
     */
    class StringResource(
        @param:StringRes val resId: Int,
        vararg val args: Any
    ) : UiText

    /**
     * Represents a plain [String] literal.
     */
    class PlainString(
        val text: String
    ) : UiText

    /**
     * Resolves the [UiText] into a [String] within a Composable function.
     */
    @Composable
    fun asString(): String {
        return when (this) {
            is StringResource -> stringResource(resId, *args)
            is PlainString -> text
        }
    }

    /**
     * Resolves the [UiText] into a [String] using a standard [Context].
     */
    fun asString(context: Context): String {
        return when (this) {
            is StringResource -> context.getString(resId, *args)
            is PlainString -> text
        }
    }
}