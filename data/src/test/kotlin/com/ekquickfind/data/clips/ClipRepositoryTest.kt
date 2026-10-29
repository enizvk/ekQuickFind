package com.ekquickfind.data.clips

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ClipRepositoryTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val day = 24L * 60 * 60 * 1000
    private var now = 100 * day

    @Test
    fun theNewestClipComesFirstAndARepeatMovesToTheFront() {
        var clips = ClipHistory.add(emptyList(), "a", 1)
        clips = ClipHistory.add(clips, "b", 2)
        clips = ClipHistory.add(clips, "a", 3)
        assertEquals(listOf("a", "b"), clips.map { it.text })
    }

    @Test
    fun onlyTheLastTenAreKept() {
        val clips = (1..12).fold(emptyList<Clip>()) { list, i -> ClipHistory.add(list, "clip $i", i.toLong()) }
        assertEquals((12 downTo 3).map { "clip $it" }, clips.map { it.text })
    }

    @Test
    fun clipsOlderThanAWeekAreDropped() {
        val clips = listOf(Clip("new", now - day), Clip("old", now - 8 * day))
        assertEquals(listOf("new"), ClipHistory.fresh(clips, now).map { it.text })
    }

    @Test
    fun rememberedClipsSurviveARestartAndCanBeForgotten() =
        runTest {
            val file = folder.root.resolve("clips.json")
            val io = StandardTestDispatcher(testScheduler)
            val first = ClipRepository(file, { now }, io)
            first.remember("hello")
            first.remember("https://example.com")
            val second = ClipRepository(file, { now }, io)
            assertEquals(listOf("https://example.com", "hello"), second.clips.first().map { it.text })
            second.forget("hello")
            assertEquals(listOf("https://example.com"), ClipRepository(file, { now }, io).clips.first().map { it.text })
        }

    @Test
    fun clearingRemovesEveryClip() =
        runTest {
            val repo = ClipRepository(folder.root.resolve("c.json"), { now }, StandardTestDispatcher(testScheduler))
            repo.remember("x")
            repo.clear()
            assertEquals(emptyList<Clip>(), repo.clips.first())
        }

    @Test
    fun blankTextIsNotRemembered() =
        runTest {
            val repo = ClipRepository(folder.root.resolve("b.json"), { now }, StandardTestDispatcher(testScheduler))
            repo.remember("   ")
            assertEquals(emptyList<Clip>(), repo.clips.first())
        }
}
