package com.example.flowdelegate

import com.example.flowdelegate.core.Data
import junit.framework.Assert.assertEquals
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import java.net.SocketTimeoutException

internal class DataFlowDelegateTestNetworkAndMemoryCache {

    private val testDispatcher = TestCoroutineDispatcher()

    @Test
    fun `fetch from network - should be cache only`() = runBlockingTest {
        val contentFromCache = "I'm from cache"
        val delegate = DataFlowDelegate<Unit, String>(
            fromNetwork = { "I'm from network" },
            fromMemory = { contentFromCache },
            workingDispatcher = testDispatcher
        )

        val expected = Data(content = contentFromCache, loading = false)
        val actual = delegate.observe(Unit, false)
            .first()

        assertEquals(expected, actual)
    }

    @Test
    fun `fetch from network - should be loading  with cache`() = runBlockingTest {
        val contentFromCache = "I'm from cache"
        val delegate = DataFlowDelegate<Unit, String>(
            fromNetwork = { "I'm from network" },
            fromMemory = { contentFromCache },
            workingDispatcher = testDispatcher
        )

        val expected = Data(content = contentFromCache, loading = true)
        val actual = delegate.observe(Unit, true)
            .first()

        assertEquals(expected, actual)
    }


    @Test
    fun `fetch from network - network error`() = runBlockingTest {
        val contentFromCache = "I'm from cache"
        val fromNetworkError = SocketTimeoutException()
        val delegate = DataFlowDelegate<Unit, String>(
            fromNetwork = { throw fromNetworkError },
            fromMemory = { contentFromCache },
            workingDispatcher = testDispatcher
        )
        val expected = Data(content = contentFromCache, error = fromNetworkError)
        val actual = delegate.observe(params = Unit, forceReload = true)
            .drop(1)
            .first()

        assertEquals(expected, actual)
    }

}