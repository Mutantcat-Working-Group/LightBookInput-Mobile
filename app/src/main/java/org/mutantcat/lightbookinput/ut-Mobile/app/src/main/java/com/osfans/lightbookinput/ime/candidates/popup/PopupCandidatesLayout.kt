/*
 * SPDX-FileCopyrightText: 2015 - 2025 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.mutantcat.lightbookinput.ime.candidates.popup

import org.mutantcat.lightbookinput.R
import org.mutantcat.lightbookinput.data.prefs.PreferenceDelegateEnum

enum class PopupCandidatesLayout(override val stringRes: Int) : PreferenceDelegateEnum {
    AUTOMATIC(R.string.automatic),
    HORIZONTAL(R.string.horizontal),
    VERTICAL(R.string.vertical),
    VERTICAL_REVERSE(R.string.vertical_reverse),
}
