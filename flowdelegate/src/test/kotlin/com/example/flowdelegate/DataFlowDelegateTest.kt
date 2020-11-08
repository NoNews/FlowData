package com.example.flowdelegate

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.replay
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert
import org.junit.Test

internal class DataFlowDelegateTest {

    private val testDispatcher = TestCoroutineDispatcher()

    @Test
    fun setUp() {
    }

    @Test
    fun `fetch from network source`() = runBlockingTest {
        val flow = MutableStateFlow("")
        flow.collect {
            Assert.assertEquals("", it)
        }
    }


}