package io.github.personan1.setloop.tips

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TipsStateTest {
    @Test
    fun freshInstallShowsOnlyTipsWithoutRules() {
        val state = TipsState()
        assertTrue(state.isEligible(Tip.ListIntro))
        assertTrue(state.isEligible(Tip.Start))
        assertTrue(state.isEligible(Tip.Block))
        assertTrue(state.isEligible(Tip.History))
        assertFalse(state.isEligible(Tip.CardActions))
        assertFalse(state.isEligible(Tip.Merge))
    }

    @Test
    fun dismissedTipNeverShowsAgain() {
        val state = TipsState().dismiss(Tip.Start)
        assertFalse(state.isEligible(Tip.Start))
        assertEquals(state, state.dismiss(Tip.Start))
    }

    @Test
    fun cardActionsNeedIntroDismissedAndATimerOpened() {
        val introClosed = TipsState().dismiss(Tip.ListIntro)
        assertFalse(introClosed.isEligible(Tip.CardActions))
        assertFalse(TipsState().openedTimer().isEligible(Tip.CardActions))
        assertTrue(introClosed.openedTimer().isEligible(Tip.CardActions))
    }

    @Test
    fun cardActionsDismissedByAnActionStayGone() {
        val state = TipsState().dismiss(Tip.ListIntro).dismiss(Tip.CardActions).openedTimer()
        assertFalse(state.isEligible(Tip.CardActions))
    }

    @Test
    fun mergeWaitsForTheNextEditorVisitAfterBlockTip() {
        val blockClosed = TipsState().dismiss(Tip.Block)
        assertFalse(blockClosed.isEligible(Tip.Merge))
        assertTrue(blockClosed.editorLeft().isEligible(Tip.Merge))
    }

    @Test
    fun leavingEditorWithBlockTipStillOpenKeepsMergeHidden() {
        assertFalse(TipsState().editorLeft().isEligible(Tip.Merge))
    }

    @Test
    fun oneTipPerScreenInListedOrder() {
        val state = TipsState().dismiss(Tip.ListIntro).openedTimer()
        assertEquals(Tip.CardActions, state.visible(Tip.ListIntro, Tip.CardActions))
        assertEquals(Tip.ListIntro, TipsState().visible(Tip.ListIntro, Tip.CardActions))
        assertNull(state.dismiss(Tip.CardActions).visible(Tip.ListIntro, Tip.CardActions))
    }

    @Test
    fun tipIdsAreUnique() {
        assertEquals(Tip.entries.size, Tip.entries.map { it.id }.toSet().size)
    }
}
