/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.mutantcat.lightbookinput.ui.setup

import android.content.Context
import androidx.fragment.app.FragmentActivity
import org.mutantcat.lightbookinput.R
import org.mutantcat.lightbookinput.data.sync.RimeDataSync
import org.mutantcat.lightbookinput.util.InputMethodUtils

enum class SetupPage {
    Mode,
    Enable,
    Select,
    ;

    fun getStepText(context: Context) = context.getText(
        when (this) {
            Mode -> R.string.setup__step_one
            Enable -> R.string.setup__step_two
            Select -> R.string.setup__step_three
        },
    )

    fun getHintText(context: Context) = context.getText(
        when (this) {
            Mode -> R.string.setup__select_data_path_hint
            Enable -> R.string.setup__enable_ime_hint
            Select -> R.string.setup__select_ime_hint
        },
    )

    fun getButtonText(context: Context) = context.getText(
        when (this) {
            Mode -> R.string.setup__select_data_path
            Enable -> R.string.setup__enable_ime
            Select -> R.string.setup__select_ime
        },
    )

    fun getButtonAction(activity: FragmentActivity) {
        when (this) {
            Mode -> (activity as SetupActivity).pickDataPath()
            Enable -> InputMethodUtils.showImeEnablerActivity(activity)
            Select -> InputMethodUtils.showImePicker()
        }
    }

    fun showActionButton(): Boolean = when (this) {
        Mode -> RimeDataSync.usesExternalSync()
        else -> true
    }

    fun isDone() = when (this) {
        Mode -> RimeDataSync.isStorageChoiceDone()
        Enable -> InputMethodUtils.checkIsLightBookInputEnabled()
        Select -> InputMethodUtils.checkIsLightBookInputSelected()
    }

    companion object {
        fun SetupPage.isLastPage() = this == entries.last()

        fun Int.isLastPage() = this == entries.size - 1

        fun hasUndonePage() = entries.any { !it.isDone() }

        fun firstUndonePage() = entries.firstOrNull { !it.isDone() }
    }
}
