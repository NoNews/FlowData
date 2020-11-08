package com.example.flowdelegate

import com.example.flowdelegate.core.Data
import com.example.flowdelegate.internal.concatWith
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*


class DataFlowDelegate<Params : Any, Domain : Any>(
    private val fromNetwork: suspend (params: Params) -> Domain,
    private val fromMemory: (params: Params) -> Domain? = { _ -> null },
    private val toMemory: (params: Params, Domain) -> Unit = { _, _ -> Unit },
    private val fromStorage: suspend (params: Params) -> Domain? = { _ -> null },
    private val toStorage: suspend (params: Params, Domain) -> Unit = { _, _ -> Unit },
    private val workingDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    private val flow = MutableSharedFlow<Data<Domain>>()

    suspend fun observe(params: Params, forceReload: Boolean): Flow<Data<Domain>> = flow {
        val fromMemory = fromMemory.invoke(params)
        val loading = fromMemory == null || forceReload
        emitAll(
            getFromStorage(params = params, fromMemoryCache = fromMemory)
                .flatMapMerge { fromStorage ->
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
        )
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
            flowOf(data)
                .concatWith(
                    merge(
                        getFromNetwork(
                            params = params,
                            storageData = fromStorage.content
                        ),
                        flow
                    )
                )
        } else {
            flowOf(fromStorage)
                .concatWith(flow)
        }
    }

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
            }.flowOn(workingDispatcher)
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
    }.flowOn(workingDispatcher)
        .catch { error ->
            val errorData = Data(
                content = storageData,
                error = error
            )
            emit(errorData)
            flow.emit(errorData)
            emitAll(flow)
        }


}
