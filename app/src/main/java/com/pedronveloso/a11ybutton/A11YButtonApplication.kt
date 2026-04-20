/*
 * Copyright (C) 2026 Pedro Veloso
 * SPDX-License-Identifier: Apache-2.0
 */
package com.pedronveloso.a11ybutton

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.pedronveloso.a11ybutton.logging.InMemoryLogTree
import com.pedronveloso.a11ybutton.notifications.ServiceStatusNotifier
import com.pedronveloso.a11ybutton.work.ServiceCheckWorker
import java.util.concurrent.TimeUnit
import timber.log.Timber

class A11YButtonApplication : Application() {
  override fun onCreate() {
    super.onCreate()

    if (BuildConfig.DEBUG) {
      Timber.plant(Timber.DebugTree())
      Timber.plant(InMemoryLogTree())
      Timber.i("Timber initialized for debug build")
    }

    ServiceStatusNotifier.createChannel(this)

    WorkManager.getInstance(this)
        .enqueueUniquePeriodicWork(
            "service_check",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<ServiceCheckWorker>(6, TimeUnit.HOURS).build(),
        )
  }
}
