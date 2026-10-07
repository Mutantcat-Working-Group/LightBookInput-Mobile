/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.mutantcat.lightbookinput.ime.bar

import org.mutantcat.lightbookinput.ime.bar.UnrollButtonStateMachine.BooleanKey.UnrolledCandidatesEmpty
import org.mutantcat.lightbookinput.ime.bar.UnrollButtonStateMachine.State.ClickToAttachWindow
import org.mutantcat.lightbookinput.ime.bar.UnrollButtonStateMachine.State.ClickToDetachWindow
import org.mutantcat.lightbookinput.ime.bar.UnrollButtonStateMachine.State.Hidden
import org.mutantcat.lightbookinput.util.BuildTransitionEvent
import org.mutantcat.lightbookinput.util.EventStateMachine
import org.mutantcat.lightbookinput.util.TransitionBuildBlock

object UnrollButtonStateMachine {
    enum class State {
        ClickToAttachWindow,
        ClickToDetachWindow,
        Hidden,
    }

    enum class BooleanKey : EventStateMachine.BooleanStateKey {
        UnrolledCandidatesEmpty,
    }

    enum class TransitionEvent(
        val builder: TransitionBuildBlock<State, BooleanKey>,
    ) : EventStateMachine.TransitionEvent<State, BooleanKey> by BuildTransitionEvent(builder) {
        UnrolledCandidatesUpdated({
            from(Hidden) transitTo ClickToAttachWindow on (UnrolledCandidatesEmpty to false)
            from(ClickToAttachWindow) transitTo Hidden on (UnrolledCandidatesEmpty to true)
        }),
        UnrolledCandidatesAttached({
            from(ClickToAttachWindow) transitTo ClickToDetachWindow
        }),
        UnrolledCandidatesDetached({
            from(ClickToDetachWindow) transitTo Hidden on (UnrolledCandidatesEmpty to true)
            from(ClickToDetachWindow) transitTo ClickToAttachWindow on (UnrolledCandidatesEmpty to false)
        }),
    }

    fun new(block: (State) -> Unit) = EventStateMachine<State, TransitionEvent, BooleanKey>(
        initialState = Hidden,
        externalBooleanStates =
        mutableMapOf(
            UnrolledCandidatesEmpty to true,
        ),
    ).apply {
        onNewStateListener = block
    }
}
