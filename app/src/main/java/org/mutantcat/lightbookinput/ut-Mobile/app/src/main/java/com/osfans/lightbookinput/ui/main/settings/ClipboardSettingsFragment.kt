/*
 * SPDX-FileCopyrightText: 2015 - 2025 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.mutantcat.lightbookinput.ui.main.settings

import org.mutantcat.lightbookinput.data.prefs.AppPrefs
import org.mutantcat.lightbookinput.data.prefs.PreferenceDelegateFragment

class ClipboardSettingsFragment : PreferenceDelegateFragment(AppPrefs.defaultInstance().clipboard)
