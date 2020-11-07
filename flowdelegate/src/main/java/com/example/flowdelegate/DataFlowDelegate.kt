package com.example.flowdelegate

import com.example.flowdelegate.core.Data
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*


class DataFlowDelegate<Params : Any, Domain : Any>(
    private val fromNetwork: suspend (params: Params) -> Domain,
    private val fromMemory: (params: Params) -> Domain? = { _ -> null },
    private val toMemory: (params: Params, Domain) -> Unit = { _, _ -> Unit },
    private val fromStorage: suspend ((params: Params) -> Domain?) = { _ -> null },
    private val toStorage: (suspend (params: Params, Domain) -> Unit) = { _, _ -> Unit }
) {

    private val flow = MutableSharedFlow<Data<Domain>>()

    @FlowPreview
    suspend fun observe(params: Params, forceReload: Boolean): Flow<Data<Domain>> {
        val fromMemory = fromMemory.invoke(params)
        val loading = fromMemory == null || forceReload
        return getFromStorage(params = params, fromMemoryCache = fromMemory)
            .flatMapConcat { fromStorage ->
                getFromNetworkIfNeeded(loading, fromStorage, params)
            }
            .catch {
                //error in database
                val dataWithLoading =
                    Data<Domain>(loading = true)
                emit(dataWithLoading)
                emitAll(flow)
            }
            .onStart {
                emit(
                    Data(
                        content = fromMemory,
                        loading = loading
                    )
                )
            }
            .distinctUntilChanged()
    }

    @FlowPreview
    private suspend fun getFromNetworkIfNeeded(
        loading: Boolean,
        fromStorage: Data<Domain>,
        params: Params
    ): Flow<Data<Domain>> {
        return if (loading) {
            val data = fromStorage.copy(loading = true)
            flow.emit(data)
            concat(
                flowOf(data),
                merge(
                    getFromNetwork(
                        params = params,
                        storageData = fromStorage.content
                    ),
                    flow
                )
            )
        } else {
            concat(
                flowOf(fromStorage),
                flow
            )
        }
    }

    @FlowPreview
    private suspend fun getFromStorage(
        params: Params,
        fromMemoryCache: Domain?
    ): Flow<Data<Domain>> {
        return if (fromMemoryCache != null) {
            flowOf(Data(fromMemoryCache))
        } else {
            flow {
                val fromStorage = fromStorage.invoke(params)
                fromStorage?.let {
                    toMemory.invoke(params, fromStorage)
                }
                emit(Data(content = fromStorage))
            }.flowOn(Dispatchers.IO)
        }
    }

    private suspend fun getFromNetwork(
        params: Params,
        storageData: Domain?
    ): Flow<Data<Domain>> = flow {
        val fromNetwork = fromNetwork.invoke(params)
        toMemory.invoke(params, fromNetwork)
        toStorage.invoke(params, fromNetwork)
        val data = Data(content = fromNetwork)
        emit(data)
    }.flowOn(Dispatchers.IO)
        .catch { error ->
            val errorData = Data(
                content = storageData,
                error = error
            )
            emit(errorData)
            flow.emit(errorData)
            emitAll(flow)
        }

    private fun <T> concat(vararg others: Flow<T>): Flow<T> {
        return flow {
            others.forEach { flow ->
                flow.collect {
                    emit(it)
                }
            }
        }
    }
}