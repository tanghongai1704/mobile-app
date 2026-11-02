package com.example.onespace.domain.games.engine

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KnowledgeQuizTest {
    private fun run() = KnowledgeQuizRun.start(random = Random(7))

    @Test fun `a run has ten questions climbing in difficulty`() {
        val r = run()
        assertEquals(KnowledgeQuizRun.LENGTH, r.questions.size)
        assertEquals(r.questions.map { it.level }, r.questions.map { it.level }.sorted())
    }

    @Test fun `every question keeps its correct answer after shuffling`() {
        val originals = KnowledgeBank.questions.associate { it.question to it.options[it.correctIndex] }
        run().questions.forEach { assertEquals(originals[it.question], it.options[it.correctIndex]) }
    }

    @Test fun `right answers add up the rewards and finish the run`() {
        val r = run()
        while (!r.over) assertTrue(r.answer(r.current!!.correctIndex))
        assertTrue(r.won)
        assertEquals(KnowledgeQuizRun.REWARDS.sum(), r.earned)
    }

    @Test fun `a wrong answer ends the run and keeps only the checkpoint`() {
        val r = run()
        repeat(6) { r.answer(r.current!!.correctIndex) }
        val wrong = (r.current!!.correctIndex + 1) % 4
        assertFalse(r.answer(wrong))
        assertTrue(r.over)
        assertEquals(KnowledgeQuizRun.REWARDS.take(5).sum(), r.earned)
        assertNull(r.current)
    }

    @Test fun `a miss before the first checkpoint keeps nothing`() {
        val r = run()
        r.answer(r.current!!.correctIndex)
        r.answer((r.current!!.correctIndex + 1) % 4)
        assertEquals(0, r.earned)
    }

    @Test fun `walking away keeps everything earned`() {
        val r = run()
        r.answer(r.current!!.correctIndex)
        r.answer(r.current!!.correctIndex)
        val before = r.earned
        r.walkAway()
        assertTrue(r.over)
        assertEquals(before, r.earned)
        assertNotNull(r.questions.firstOrNull())
    }
}
