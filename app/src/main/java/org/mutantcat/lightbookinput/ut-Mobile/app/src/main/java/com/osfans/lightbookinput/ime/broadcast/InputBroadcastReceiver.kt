/*
 * SPDX-FileCopyrightText: 2015 - 2025 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.mutantcat.lightbookinput.ime.broadcast

import android.view.inputmethod.EditorInfo
import org.mutantcat.lightbookinput.core.Candidates
import org.mutantcat.lightbookinput.core.CompositionProto
import org.mutantcat.lightbookinput.core.RimeMessage
import org.mutantcat.lightbookinput.core.SchemaItem
import org.mutantcat.lightbookinput.core.StatusProto
import org.mutantcat.lightbookinput.ime.window.BoardWindow

interface InputBroadcastReceiver {
    fun onStartInput(info: EditorInfo) {}

    fun onSelectionUpdate(start: Int, end: Int) {}

    fun onRimeSchemaUpdated(schema: SchemaItem) {}

    fun onRimeOptionUpdated(value: RimeMessage.OptionMessage.Data) {}

    fun onCandidateListUpdate(data: Candidates.Bulk) {}

    fun onCompositionUpdate(data: CompositionProto) {}

    fun onKeyAppearanceUpdate(composing: Boolean, menu: Boolean, paging: Boolean) {}

    fun onInputStatusUpdate(value: StatusProto) {}

    fun onWindowAttached(window: BoardWindow) {}

    fun onWindowDetached(window: BoardWindow) {}

    fun onEnterKeyLabelUpdate(label: String) {}
}
