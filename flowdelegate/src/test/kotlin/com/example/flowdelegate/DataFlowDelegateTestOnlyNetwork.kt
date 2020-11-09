package com.example.flowdelegate

import com.example.flowdelegate.core.Data
import junit.framework.Assert.assertEquals
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import java.net.SocketTimeoutException

internal class DataFlowDelegateTestOnlyNetwork {

    private val testDispatcher = TestCoroutineDispatcher()

    @Test
    fun `fetch from network - should be loading first`() = runBlockingTest {
        val delegate = DataFlowDelegate<Unit, String>(
            fromNetwork = { "I'm from network" },
            workingDispatcher = testDispatcher
        )

        val expected = Data<String>(loading = true)
        val actual = delegate.observe(Unit, false)
            .first()

        assertEquals(expected, actual)
    }

    @Test
    fun `fetch from network - should be network data after progress`() = runBlockingTest {
        val fromNetworkContent = "I'm from network!"
        val delegate = DataFlowDelegate<Unit, String>(
            fromNetwork = { fromNetworkContent },
            workingDispatcher = testDispatcher
        )
        val expected = Data(content = fromNetworkContent)
        val actual = delegate.observe(Unit, false)
            .drop(1)
            .first()

        assertEquals(expected, actual)
    }

    @Test
    fun `fetch from network - network error`() = runBlockingTest {
        val fromNetworkError = SocketTimeoutException()
        val delegate = DataFlowDelegate<Unit, String>(
            fromNetwork = { throw fromNetworkError },
            workingDispatcher = testDispatcher
        )
        val expected = Data<String>(error = fromNetworkError)
        val actual = delegate.observe(Unit, false)
            .drop(1)
            .first()

        assertEquals(expected, actual)
    }

}