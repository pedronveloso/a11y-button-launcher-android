/*
 * Copyright (C) 2026 Pedro Veloso
 * All rights reserved.
 */
package com.pedronveloso.a11ybutton

import android.app.Application
import com.pedronveloso.a11ybutton.logging.InMemoryLogTree
import timber.log.Timber

class A11YButtonApplication : Application() {
  override fun onCreate() {
    super.onCreate()

    if (BuildConfig.DEBUG) {
      Timber.plant(Timber.DebugTree())
      Timber.plant(InMemoryLogTree())
      Timber.i("Timber initialized for debug build")
    }
  }
}
