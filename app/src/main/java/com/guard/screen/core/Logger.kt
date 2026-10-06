package com.guard.screen.core

import android.util.Log
import com.guard.screen.BuildConfig
import timber.log.Timber

object Logger {

    private const val TAG = "ScreenGuard"
    private var initialized = false

    fun init() {
        if (initialized) return
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(ReleaseTree())
        }
        initialized = true
    }

    fun d(msg: String) {
        if (initialized) Timber.d(msg) else Log.d(TAG, msg)
    }

    fun d(tag: String, msg: String) {
        if (initialized) Timber.tag(tag).d(msg) else Log.d("$TAG/$tag", msg)
    }

    fun i(msg: String) {
        if (initialized) Timber.i(msg) else Log.i(TAG, msg)
    }

    fun i(tag: String, msg: String) {
        if (initialized) Timber.tag(tag).i(msg) else Log.i("$TAG/$tag", msg)
    }

    fun w(msg: String) {
        if (initialized) Timber.w(msg) else Log.w(TAG, msg)
    }

    fun w(tag: String, msg: String) {
        if (initialized) Timber.tag(tag).w(msg) else Log.w("$TAG/$tag", msg)
    }

    fun e(msg: String) {
        if (initialized) Timber.e(msg) else Log.e(TAG, msg)
    }

    fun e(tag: String, msg: String) {
        if (initialized) Timber.tag(tag).e(msg) else Log.e("$TAG/$tag", msg)
    }

    fun e(tag: String, msg: String, t: Throwable) {
        if (initialized) Timber.tag(tag).e(t, msg) else Log.e("$TAG/$tag", msg, t)
    }

    fun e(t: Throwable) {
        if (initialized) Timber.e(t) else Log.e(TAG, "Error", t)
    }

    private class ReleaseTree : Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            if (priority >= Log.ERROR) {
                Log.e(tag ?: TAG, message, t)
            }
        }
    }
}
