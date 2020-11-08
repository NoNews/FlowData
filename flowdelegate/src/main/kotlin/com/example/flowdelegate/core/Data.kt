package com.example.flowdelegate.core

data class Data<T>(
    val content: T? = null,
    val error: Throwable? = null,
    val loading: Boolean = false
)