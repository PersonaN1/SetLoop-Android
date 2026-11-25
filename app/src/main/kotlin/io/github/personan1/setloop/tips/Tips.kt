package io.github.personan1.setloop.tips

/** First-run hints (SPEC D23, DESIGN V12), ported from iOS `Tips.swift`. [id] is the persisted key. */
enum class Tip(val id: String) {
    /** Top of the timer list on first launch. */
    ListIntro("listIntro"),

    /** Long press on a card is invisible; shown after the user has opened a timer. */
    CardActions("cardActions"),

    /** On the preview's Start button. */
    Start("start"),

    /** Top of the editor: blocks and cycles. */
    Block("block"),

    /** On "Select steps", at the next editor visit after the block tip is closed. */
    Merge("merge"),

    /** On the summary after the first workout. */
    History("history"),
}

/**
 * What the user has seen and done, and the display rules over it. Immutable: every event returns a new state.
 * The flags are the TipKit `@Parameter`s of the iOS app.
 */
data class TipsState(
    val dismissed: Set<String> = emptySet(),
    val introDone: Boolean = false,
    val openedTimer: Boolean = false,
    val blockTipDone: Boolean = false,
) {
    fun isEligible(tip: Tip): Boolean = tip.id !in dismissed && when (tip) {
        Tip.CardActions -> introDone && openedTimer
        Tip.Merge -> blockTipDone
        Tip.ListIntro, Tip.Start, Tip.Block, Tip.History -> true
    }

    /** At most one tip per screen: the first eligible of [candidates], in the order the screen lists them. */
    fun visible(vararg candidates: Tip): Tip? = candidates.firstOrNull(::isEligible)

    /** The close button, or the action the tip speaks about. */
    fun dismiss(tip: Tip): TipsState = copy(
        dismissed = dismissed + tip.id,
        introDone = introDone || tip == Tip.ListIntro,
    )

    /** The preview was opened: back on the list, the card actions tip may show. */
    fun openedTimer(): TipsState = copy(openedTimer = true)

    /** The editor was left. The merge tip waits for the next visit instead of showing right after the block tip. */
    fun editorLeft(): TipsState = if (Tip.Block.id in dismissed) copy(blockTipDone = true) else this
}
