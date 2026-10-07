/*
 * SPDX-FileCopyrightText: 2015 - 2024 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.mutantcat.lightbookinput.ime.core

import androidx.annotation.StringRes
import org.mutantcat.lightbookinput.R
import org.mutantcat.lightbookinput.data.prefs.PreferenceDelegateEnum

enum class InlinePreeditMode(
    @StringRes override val stringRes: Int,
) : PreferenceDelegateEnum {
    DISABLE(R.string.disable),
    COMPOSING_TEXT(R.string.composing_text),
    COMMIT_TEXT_PREVIEW(R.string.commit_text_preview),
}
