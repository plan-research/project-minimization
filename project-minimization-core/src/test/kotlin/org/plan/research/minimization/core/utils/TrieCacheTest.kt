package org.plan.research.minimization.core.utils

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.random.Random

class TrieCacheTest {

    @Test
    fun emptyTest() {
        val cache = TrieCache<Int, Unit>()
        assertNull(cache[emptyList()])
        assertNull(cache[listOf(1, 2, 3, 4, 5)])
    }

    @Test
    fun emptyKeyTest() {
        val cache = TrieCache<Int, Unit>()
        assertNull(cache[emptyList()])
        cache[emptyList()] = Unit
        assertNotNull(cache[emptyList()])
    }

    @Test
    fun setTest() {
        val cache = TrieCache<Int, Unit>()
        val key = listOf(1, 2, 3, 4, 5)
        assertNull(cache[key])
        cache[key] = Unit
        testSingleKey(cache, key)
    }

    @Test
    fun getOrUpdateTest() {
        val cache = TrieCache<Int, Unit>()
        val key = listOf(1, 2, 3, 4, 5)
        assertNull(cache[key])
        cache.getOrPut(key) { }
        testSingleKey(cache, key)
    }

    @Test
    fun orderingTest() {
        val random = Random(42)
        val cache = TrieCache<Int, Unit>()
        val key = listOf(1, 2, 3, 4, 5)
        assertNull(cache[key])
        cache[key] = Unit
        assertNotNull(cache[key])
        repeat(5) {
            assertNotNull(cache[key.shuffled(random)])
        }
        testSingleKey(cache, key)
    }


    private fun testSingleKey(cache: TrieCache<Int, Unit>, key: List<Int>) {
        assertNotNull(cache[key])
        for (i in 0 until key.lastIndex) {
            assertNull(cache[key.take(i)])
        }
    }
}