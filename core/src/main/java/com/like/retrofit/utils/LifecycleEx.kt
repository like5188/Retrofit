package com.like.retrofit.utils

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import com.like.retrofit.livedata.CallLiveData
import kotlinx.coroutines.Job
import retrofit2.Call
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Creates an [AutoClearedValue] associated with this LifecycleOwner.
 */
fun <T : Any> LifecycleOwner.autoCleared() = AutoClearedValue<T>(this)

/**
 * A lazy property that gets cleaned up when the lifecycleOwner is destroyed.
 *
 * Accessing this variable in a destroyed lifecycleOwner will throw NPE.
 */
class AutoClearedValue<T : Any>(lifecycleOwner: LifecycleOwner) : ReadWriteProperty<LifecycleOwner, T> {
    private var _value: T? = null

    init {
        lifecycleOwner.lifecycle.addObserver(object : LifecycleObserver {
            @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            fun onDestroy() {
                _value = null
            }
        })
    }

    override fun getValue(thisRef: LifecycleOwner, property: KProperty<*>): T {
        return _value ?: throw IllegalStateException("should never call auto-cleared-value get when it might not be available")
    }

    override fun setValue(thisRef: LifecycleOwner, property: KProperty<*>, value: T) {
        _value = value
    }
}

@JvmOverloads
fun <T> Call<T>?.bindToLifecycleOwner(
    lifecycleOwner: LifecycleOwner,
    vararg events: Lifecycle.Event = arrayOf(Lifecycle.Event.ON_DESTROY)
): Call<T>? {
    this ?: return this
    bindToLifecycleOwner(lifecycleOwner, events) {
        if (!this.isCanceled) {
            this.cancel()
        }
    }
    return this
}

@JvmOverloads
fun <T> CallLiveData<T>?.bindToLifecycleOwner(
    lifecycleOwner: LifecycleOwner,
    vararg events: Lifecycle.Event = arrayOf(Lifecycle.Event.ON_DESTROY)
): CallLiveData<T>? {
    this ?: return this
    bindToLifecycleOwner(lifecycleOwner, events) {
        if (!this.isCanceled) {
            this.cancel()
        }
    }
    return this
}

@JvmOverloads
fun <T> Job?.bindToLifecycleOwner(
    lifecycleOwner: LifecycleOwner,
    vararg events: Lifecycle.Event = arrayOf(Lifecycle.Event.ON_DESTROY)
): Job? {
    this ?: return this
    bindToLifecycleOwner(lifecycleOwner, events) {
        if (!this.isCancelled) {
            this.cancel()
        }
    }
    return this
}

private fun bindToLifecycleOwner(
    lifecycleOwner: LifecycleOwner,
    events: Array<out Lifecycle.Event>,
    cancel: () -> Unit
) {
    lifecycleOwner.lifecycle.addObserver(object : LifecycleObserver {
        @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
        fun onCreate() {
            if (events.contains(Lifecycle.Event.ON_CREATE))
                cancel()
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_START)
        fun onStart() {
            if (events.contains(Lifecycle.Event.ON_START))
                cancel()
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
        fun onResume() {
            if (events.contains(Lifecycle.Event.ON_RESUME))
                cancel()
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        fun onPause() {
            if (events.contains(Lifecycle.Event.ON_PAUSE))
                cancel()
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
        fun onStop() {
            if (events.contains(Lifecycle.Event.ON_STOP))
                cancel()
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        fun onDestroy() {
            if (events.contains(Lifecycle.Event.ON_DESTROY))
                cancel()
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_ANY)
        fun onAny() {
            if (events.contains(Lifecycle.Event.ON_ANY))
                cancel()
        }
    })
}