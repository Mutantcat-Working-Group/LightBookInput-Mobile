/*
 * SPDX-FileCopyrightText: 2015 - 2025 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.mutantcat.lightbookinput.ime.candidates.unrolled.window

import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RectShape
import android.view.View
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.recyclerview.widget.RecyclerView
import org.mutantcat.lightbookinput.daemon.RimeSession
import org.mutantcat.lightbookinput.daemon.launchOnReady
import org.mutantcat.lightbookinput.data.theme.Theme
import org.mutantcat.lightbookinput.data.theme.ThemeScope
import org.mutantcat.lightbookinput.ime.bar.InputBarDelegate
import org.mutantcat.lightbookinput.ime.bar.UnrollButtonStateMachine
import org.mutantcat.lightbookinput.ime.broadcast.InputBroadcastReceiver
import org.mutantcat.lightbookinput.ime.candidates.CandidateViewHolder
import org.mutantcat.lightbookinput.ime.candidates.compact.CompactCandidateDelegate
import org.mutantcat.lightbookinput.ime.candidates.unrolled.CandidatesPagingSource
import org.mutantcat.lightbookinput.ime.candidates.unrolled.PagingCandidateViewAdapter
import org.mutantcat.lightbookinput.ime.candidates.unrolled.UnrolledCandidateLayout
import org.mutantcat.lightbookinput.ime.core.InputView
import org.mutantcat.lightbookinput.ime.core.LightBookInputInputMethodService
import org.mutantcat.lightbookinput.ime.keyboard.KeyboardWindow
import org.mutantcat.lightbookinput.ime.window.BoardWindow
import org.mutantcat.lightbookinput.ime.window.BoardWindowManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.kodein.di.DI
import org.kodein.di.instance
import splitties.dimensions.dp
import kotlin.math.max

abstract class BaseUnrolledCandidateWindow(di: DI) :
    BoardWindow.NoBarBoardWindow(di),
    InputBroadcastReceiver {
    protected val service: LightBookInputInputMethodService by instance()
    protected val rime: RimeSession by instance()
    protected val scope: ThemeScope by instance()
    private val inputView: InputView by instance()
    private val bar: InputBarDelegate by instance()
    private val windowManager: BoardWindowManager by instance()
    private val compactCandidate: CompactCandidateDelegate by instance()

    protected val theme: Theme
        get() = scope.theme

    private lateinit var lifecycleCoroutineScope: LifecycleCoroutineScope
    private lateinit var candidateLayout: UnrolledCandidateLayout

    protected val separatorDrawable by lazy {
        ShapeDrawable(RectShape()).apply {
            val spacing = theme.style.candidateSpacing
            val intrinsicSize = max(spacing, context.dp(spacing)).toInt()
            intrinsicWidth = intrinsicSize
            intrinsicHeight = intrinsicSize
            paint.color = scope.colors.candidateSeparatorColor
        }
    }

    override fun refreshColors() {
        if (!::candidateLayout.isInitialized) return
        // the decorations share this drawable, so re-coloring its paint repaints the dividers
        separatorDrawable.paint.color = scope.colors.candidateSeparatorColor
        candidateLayout.refreshColors()
        // visible rows re-apply their colors on rebind
        adapter.notifyDataSetChanged()
    }

    abstract fun onCreateCandidateLayout(): UnrolledCandidateLayout

    final override fun onCreateView(): View {
        candidateLayout =
            onCreateCandidateLayout().apply {
                recyclerView.apply {
                    // disable item cross-fade animation
                    itemAnimator = null
                }
            }
        return candidateLayout
    }

    abstract val adapter: PagingCandidateViewAdapter
    abstract val layoutManager: RecyclerView.LayoutManager

    private var offsetJob: Job? = null

    private val candidatesPager by lazy {
        Pager(
            config = PagingConfig(
                pageSize = 48,
                enablePlaceholders = false,
            ),
            pagingSourceFactory = {
                CandidatesPagingSource(
                    rime,
                    total = compactCandidate.adapter.total,
                    offset = adapter.offset,
                )
            },
        )
    }

    private var candidatesSubmitJob: Job? = null

    override fun onAttached() {
        lifecycleCoroutineScope = candidateLayout.findViewTreeLifecycleOwner()!!.lifecycleScope
        bar.unrollButtonStateMachine.push(UnrollButtonStateMachine.TransitionEvent.UnrolledCandidatesAttached)
        offsetJob =
            lifecycleCoroutineScope.launch {
                compactCandidate.unrolledCandidateOffset.collect {
                    if (it <= 0) {
                        windowManager.attachWindow(KeyboardWindow)
                    } else {
                        candidateLayout.resetPosition()
                        adapter.refreshWith(
                            offset = it,
                            highlightedIndex = compactCandidate.adapter.highlightedIdx,
                        )
                    }
                }
            }
        candidatesSubmitJob =
            lifecycleCoroutineScope.launch {
                candidatesPager.flow.collectLatest {
                    adapter.submitData(it)
                }
            }
    }

    fun bindCandidateUiViewHolder(holder: CandidateViewHolder) {
        holder.itemView.run {
            setOnClickListener { _ ->
                rime.launchOnReady { it.selectCandidate(holder.idx, global = true) }
            }
            setOnLongClickListener { view ->
                inputView.showCandidateActionMenu(holder.idx, holder.text, view, global = true)
                true
            }
        }
    }

    override fun onDetached() {
        bar.unrollButtonStateMachine.push(
            UnrollButtonStateMachine.TransitionEvent.UnrolledCandidatesDetached,
            UnrollButtonStateMachine.BooleanKey.UnrolledCandidatesEmpty to
                (compactCandidate.adapter.total == adapter.offset),
        )
        offsetJob?.cancel()
        candidatesSubmitJob?.cancel()
    }
}
