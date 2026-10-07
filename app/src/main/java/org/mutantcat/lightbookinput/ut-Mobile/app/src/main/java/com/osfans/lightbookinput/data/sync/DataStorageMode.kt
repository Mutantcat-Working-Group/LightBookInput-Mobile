/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.mutantcat.lightbookinput.data.sync

import org.mutantcat.lightbookinput.R
import org.mutantcat.lightbookinput.data.prefs.PreferenceDelegateEnum

enum class DataStorageMode(override val stringRes: Int) : PreferenceDelegateEnum {
    EXTERNAL_SYNC(R.string.sync_from_external),
    APP_STORAGE(R.string.use_app_specific_storage),
}
