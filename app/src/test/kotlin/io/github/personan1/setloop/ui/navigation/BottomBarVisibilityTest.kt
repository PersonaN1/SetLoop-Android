package io.github.personan1.setloop.ui.navigation

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BottomBarVisibilityTest {
    @Test
    fun visibleUntilHidden() {
        val visibility = BottomBarVisibility()
        assertFalse(visibility.hidden)
        visibility.hide()
        assertTrue(visibility.hidden)
        visibility.release()
        assertFalse(visibility.hidden)
    }

    @Test
    fun staysHiddenUntilEveryRequestIsReleased() {
        val visibility = BottomBarVisibility()
        visibility.hide()
        visibility.hide()
        visibility.release()
        assertTrue(visibility.hidden)
        visibility.release()
        assertFalse(visibility.hidden)
    }
}
