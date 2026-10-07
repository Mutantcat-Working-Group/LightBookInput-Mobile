/*
 * SPDX-FileCopyrightText: 2015 - 2024 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.mutantcat.lightbookinput.util

import android.content.Context
import org.mutantcat.lightbookinput.LightBookInputApplication

val appContext: Context get() = LightBookInputApplication.getInstance().applicationContext
