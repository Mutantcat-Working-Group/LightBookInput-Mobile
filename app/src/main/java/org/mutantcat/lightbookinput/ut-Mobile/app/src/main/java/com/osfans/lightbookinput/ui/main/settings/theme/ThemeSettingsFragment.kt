/*
 * SPDX-FileCopyrightText: 2015 - 2025 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.mutantcat.lightbookinput.ui.main.settings.theme

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import org.mutantcat.lightbookinput.R
import org.mutantcat.lightbookinput.data.prefs.PreferenceDelegateFragment
import org.mutantcat.lightbookinput.data.theme.ThemeManager
import org.mutantcat.lightbookinput.ui.main.settings.ColorPickerDialog
import org.mutantcat.lightbookinput.ui.main.settings.ThemePickerDialog
import org.mutantcat.lightbookinput.util.addPreference
import org.mutantcat.lightbookinput.util.startActivity
import kotlinx.coroutines.launch

class ThemeSettingsFragment : PreferenceDelegateFragment(ThemeManager.prefs) {
    override fun onCreatePreferences(
        savedInstanceState: Bundle?,
        rootKey: String?,
    ) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        findPreference<Preference>("selected_theme")?.setOnPreferenceClickListener {
            lifecycleScope.launch { ThemePickerDialog.build(lifecycleScope, requireContext()).show() }
            true
        }
        findPreference<Preference>("normal_mode_color")?.setOnPreferenceClickListener {
            lifecycleScope.launch { ColorPickerDialog.build(lifecycleScope, requireContext()).show() }
            true
        }
    }

    override fun onPreferenceUiCreated(screen: PreferenceScreen) {
        screen.addPreference(
            R.string.theme_diagnostics,
            R.string.theme_diagnostics_summary,
        ) {
            startActivity<ThemeDiagnosticsActivity>()
        }
    }
}
