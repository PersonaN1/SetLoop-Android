package io.github.personan1.setloop.ui.timers

import io.github.personan1.setloop.data.SetLoopDatabase
import io.github.personan1.setloop.data.TEST_SDK
import io.github.personan1.setloop.data.TickingClock
import io.github.personan1.setloop.data.TimerAddResult
import io.github.personan1.setloop.data.TimerRepository
import io.github.personan1.setloop.data.inMemoryDatabase
import io.github.personan1.setloop.data.sampleDefinition
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TEST_SDK])
class TimerListViewModelTest {
    private lateinit var database: SetLoopDatabase
    private lateinit var repository: TimerRepository

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        database = inMemoryDatabase()
        repository = TimerRepository(database, TickingClock(), maxTimers = 3)
    }

    @AfterTest
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    private suspend fun addTimers(vararg names: String) = names.map {
        assertIs<TimerAddResult.Added>(repository.add(it, sampleDefinition(), "New")).timer.id
    }

    private fun withViewModel(block: suspend (TimerListViewModel) -> Unit) = runBlocking {
        val viewModel = TimerListViewModel(repository)
        val collector = launch(Dispatchers.Default) { viewModel.state.collect {} }
        try {
            withTimeout(TIMEOUT_MS) { block(viewModel) }
        } finally {
            collector.cancel()
        }
    }

    private suspend fun TimerListViewModel.awaitNames(vararg names: String) =
        state.first { state -> state.timers?.map { it.name } == names.toList() }

    private suspend fun storedNames() = repository.observeTimers().first().map { it.name }

    @Test
    fun `shows the stored timers in order`() = withViewModel { viewModel ->
        addTimers("A", "B")
        viewModel.awaitNames("A", "B")
    }

    @Test
    fun `dragging reorders at once and saving persists the order`() = withViewModel { viewModel ->
        val (a, _, c) = addTimers("A", "B", "C")
        viewModel.awaitNames("A", "B", "C")

        viewModel.move(a, c)
        assertEquals(listOf("B", "C", "A"), viewModel.state.value.timers?.map { it.name })
        assertEquals(listOf("A", "B", "C"), storedNames())

        viewModel.saveOrder()
        while (storedNames() != listOf("B", "C", "A")) delay(10)
        viewModel.awaitNames("B", "C", "A")
    }

    @Test
    fun `move by one place persists straight away`() = withViewModel { viewModel ->
        val (_, b, _) = addTimers("A", "B", "C")
        viewModel.awaitNames("A", "B", "C")

        viewModel.moveBy(b, -1)
        while (storedNames() != listOf("B", "A", "C")) delay(10)
    }

    @Test
    fun `deleting selected timers keeps selection mode while timers remain`() = withViewModel { viewModel ->
        val (a, b, _) = addTimers("A", "B", "C")
        viewModel.awaitNames("A", "B", "C")

        viewModel.startSelecting()
        viewModel.toggleSelected(a)
        viewModel.toggleSelected(b)
        viewModel.deleteSelected()

        val state = viewModel.state.first { it.timers?.size == 1 && it.selection.selected.isEmpty() }
        assertTrue(state.selection.active)
        assertEquals(listOf("C"), state.timers?.map { it.name })
    }

    @Test
    fun `deleting every timer ends selection mode`() = withViewModel { viewModel ->
        addTimers("A", "B")
        viewModel.awaitNames("A", "B")
        viewModel.startSelecting()
        viewModel.toggleAll()
        viewModel.deleteSelected()

        val state = viewModel.state.first { it.timers?.isEmpty() == true }
        assertFalse(state.selection.active)
    }

    @Test
    fun `duplicate at the limit shows the limit message`() = withViewModel { viewModel ->
        val (a, _, _) = addTimers("A", "B", "C")
        viewModel.awaitNames("A", "B", "C")

        viewModel.duplicate(a)
        assertTrue(viewModel.state.first { it.limitReached }.limitReached)
        viewModel.dismissLimit()
        assertFalse(viewModel.state.first { !it.limitReached }.limitReached)
    }

    @Test
    fun `duplicate puts the copy after the original`() = withViewModel { viewModel ->
        val (a, _) = addTimers("A", "B")
        viewModel.awaitNames("A", "B")
        viewModel.duplicate(a)
        viewModel.awaitNames("A", "A (2)", "B")
    }

    private companion object {
        const val TIMEOUT_MS = 10_000L
    }
}
