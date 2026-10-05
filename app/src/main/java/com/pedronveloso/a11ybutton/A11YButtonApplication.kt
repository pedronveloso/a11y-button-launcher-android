/*
 * Copyright (C) 2026 Pedro Veloso
 * SPDX-License-Identifier: Apache-2.0
 */
package com.pedronveloso.a11ybutton

import android.app.Application
import com.pedronveloso.a11ybutton.data.SettingsRepository
import com.pedronveloso.a11ybutton.logging.LoggingController
import com.pedronveloso.a11ybutton.notifications.ServiceStatusNotifier
import timber.log.Timber

class A11YButtonApplication : Application() {
  lateinit var loggingController: LoggingController
    private set

  override fun onCreate() {
    super.onCreate()

    if (BuildConfig.DEBUG) {
      Timber.plant(Timber.DebugTree())
      Timber.i("Timber initialized for debug build")
    }

    loggingController = LoggingController(this, SettingsRepository.fromContext(this))
    ServiceStatusNotifier.createChannel(this)
  }
}
