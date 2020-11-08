package com.example.dataflowdelegate.core

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.dataflowdelegate.R
import com.example.flowdelegate.DataFlowDelegate
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.concurrent.TimeoutException

class MainActivity : AppCompatActivity(R.layout.activity_main) {


    private val delegate = DataFlowDelegate<Unit, String>(
        fromNetwork = { throw TimeoutException() },
        fromMemory = { null },
        fromStorage = { "fromStorage" },
        workingDispatcher = Dispatchers.IO
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        CoroutineScope(Dispatchers.Main.immediate).launch {
            delegate.observe(params = Unit, forceReload = true)
                .collect {

                }
        }



        update.setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch {
                delegate.observe(Unit, false)
                    .filter { !it.loading }
                    .firstOrNull()
            }

        }

    }
}