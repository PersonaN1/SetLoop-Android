package io.github.personan1.setloop.tips

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class TipsStoreTest {
    @Test
    fun dismissalSurvivesANewStoreOnTheSameFile() = runBlocking {
        val file = File(createTempDirectory().toFile(), "tips.preferences_pb")
        val first = openStore(file)
        first.store.dismiss(Tip.ListIntro)
        first.store.openedTimer()
        first.scope.cancel()

        val second = openStore(file)
        val state = second.store.state.first()
        assertTrue(Tip.ListIntro.id in state.dismissed)
        assertTrue(state.introDone)
        assertTrue(state.openedTimer)
        second.scope.cancel()
    }

    @Test
    fun showAgainResetsEverything() = runBlocking {
        val opened = openStore(File(createTempDirectory().toFile(), "tips.preferences_pb"))
        opened.store.dismiss(Tip.Block)
        opened.store.editorLeft()
        opened.store.showAgain()
        assertEquals(TipsState(), opened.store.state.first())
        opened.scope.cancel()
    }

    private class Opened(val store: TipsStore, val scope: CoroutineScope)

    private fun openStore(file: File): Opened {
        val scope = CoroutineScope(Dispatchers.IO)
        return Opened(TipsStore(PreferenceDataStoreFactory.create(scope = scope) { file }), scope)
    }
}
