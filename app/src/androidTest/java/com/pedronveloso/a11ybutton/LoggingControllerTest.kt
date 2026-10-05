/*
 * Copyright (C) 2026 Pedro Veloso
 * SPDX-License-Identifier: Apache-2.0
 */
package com.pedronveloso.a11ybutton

import android.content.Context
import android.content.ContextWrapper
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pedronveloso.a11ybutton.data.SettingsRepository
import com.pedronveloso.a11ybutton.logging.LoggingController
import com.pedronveloso.a11ybutton.logging.LoggingError
import com.pedronveloso.logviewer.TimberLogCapture
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import timber.log.Timber

@RunWith(AndroidJUnit4::class)
class LoggingControllerTest {
  @Test
  fun disabledDefault_doesNotInitializeStorage() = runBlocking {
    withController(default = false) { controller, _, context ->
      awaitSettled(controller)
      assertFalse(controller.state.value.enabled)
      assertNull(controller.capture)
      assertFalse(File(context.noBackupFilesDir, "pnv-logviewer").exists())
    }
  }

  @Test
  fun capture_redactsText_preservesExceptions_clearsPreviousSessions_andAvoidsDuplicateTrees() =
      runBlocking {
        withController(default = true, seedPrevious = true) { controller, repository, _ ->
          awaitSettled(controller)
          val source = requireNotNull(controller.capture)
          val current = source.sessions().first { it.isCurrent }
          val previous = source.sessions().first { !it.isCurrent }
          assertTrue(source.entries(previous.id).any { it.message == "Previous session" })
          val treeCount = Timber.treeCount
          Timber.tag("logging-test")
              .e(IllegalStateException("original exception"), "Original user@example.com")
          val captured = source.entries(current.id).filter { it.tag == "logging-test" }
          assertEquals(1, captured.size)
          assertEquals("Original <redacted>", captured.single().message)
          assertTrue(captured.single().throwableStackTrace!!.contains("original exception"))
          repeat(3) {
            controller.setEnabled(false)
            awaitSettled(controller)
            assertFalse(repository.settings.first().inAppLoggingEnabled)
            assertFalse(repository.settings.first().loggingCleanupPending)
            assertFalse(source.health.value.installed!!)
            assertTrue(source.sessions().all { it.isCurrent && source.entries(it.id).isEmpty() })
            Timber.tag("disabled-test").i("Must not be captured")
            controller.setEnabled(true)
            awaitSettled(controller)
            assertEquals(treeCount, Timber.treeCount)
            assertTrue(
                source.entries(current.id).none {
                  it.tag == "disabled-test" || it.tag == "logging-test"
                }
            )
          }
          Timber.tag("single-tree-test").i("One emission")
          assertEquals(1, source.entries(current.id).count { it.tag == "single-tree-test" })
        }
      }

  @Test
  fun deletionFailure_keepsCaptureDisabledAndCleanupPending() = runBlocking {
    withController(default = true) { controller, repository, context ->
      awaitSettled(controller)
      val source = requireNotNull(controller.capture)
      source.sessions() // Wait for disk initialization before making deletion fail.
      val journal = File(context.noBackupFilesDir, "pnv-logviewer")
      val sessionFile = journal.listFiles()!!.first()
      assertTrue(sessionFile.delete())
      assertTrue(sessionFile.mkdir())
      File(sessionFile, "block-deletion").writeText("test")
      controller.setEnabled(false)
      awaitSettled(controller)
      assertFalse(controller.state.value.enabled)
      assertEquals(LoggingError.Cleanup, controller.state.value.error)
      assertFalse(repository.settings.first().inAppLoggingEnabled)
      assertTrue(repository.settings.first().loggingCleanupPending)
      assertFalse(source.health.value.installed!!)
      assertNotNull(source.health.value.writeError)
      controller.setEnabled(true)
      awaitSettled(controller)
      assertFalse(controller.state.value.enabled)
    }
  }

  @Test
  fun enableFailure_keepsPersistedSettingOffAndReportsEnableError() = runBlocking {
    val store = FailableStore()
    withController(default = false, store = store) { controller, repository, _ ->
      awaitSettled(controller)
      store.failWrites = true
      controller.setEnabled(true)
      awaitSettled(controller)
      store.failWrites = false
      assertFalse(controller.state.value.enabled)
      assertEquals(LoggingError.Enable, controller.state.value.error)
      assertFalse(repository.settings.first().inAppLoggingEnabled)
      assertFalse(requireNotNull(controller.capture).health.value.installed!!)
    }
  }

  @Test
  fun interruptedDisable_resumesCleanupOnLaunch() = runBlocking {
    withController(default = true, seedPrevious = true, pendingCleanup = true) {
        controller,
        repository,
        _ ->
      awaitSettled(controller)
      assertFalse(controller.state.value.enabled)
      assertFalse(repository.settings.first().loggingCleanupPending)
      val source = requireNotNull(controller.capture)
      assertTrue(source.sessions().all { it.isCurrent && source.entries(it.id).isEmpty() })
    }
  }

  private suspend fun awaitSettled(controller: LoggingController) {
    withTimeout(10_000) { controller.state.first { !it.transitioning } }
  }

  private suspend fun withController(
      default: Boolean,
      seedPrevious: Boolean = false,
      pendingCleanup: Boolean = false,
      store: FailableStore = FailableStore(),
      test: suspend (LoggingController, SettingsRepository, Context) -> Unit,
  ) {
    val base = ApplicationProvider.getApplicationContext<Context>()
    val root = File(base.cacheDir, "logging-test-${System.nanoTime()}").apply { mkdirs() }
    val context =
        object : ContextWrapper(base) {
          override fun getApplicationContext(): Context = this

          override fun getNoBackupFilesDir(): File = root
        }
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val repository =
        SettingsRepository(
            store.also {
              it.delegate =
                  PreferenceDataStoreFactory.create(
                      scope = scope,
                      produceFile = { File(root, "settings.preferences_pb") },
                  )
            },
            loggingDefault = default,
        )
    if (seedPrevious) {
      val previous = TimberLogCapture(context, persistAcrossCrashes = true)
      previous.install()
      Timber.i("Previous session")
      previous.uninstall()
      previous.sessions()
    }
    if (pendingCleanup) repository.setInAppLoggingEnabled(false)
    val controller = LoggingController(context, repository, scope)
    try {
      test(controller, repository, context)
    } finally {
      controller.capture?.uninstall()
      scope.cancel()
      root.deleteRecursively()
    }
  }

  private class FailableStore : DataStore<Preferences> {
    lateinit var delegate: DataStore<Preferences>
    @Volatile var failWrites = false
    override val data
      get() = delegate.data

    override suspend fun updateData(
        transform: suspend (t: Preferences) -> Preferences
    ): Preferences {
      if (failWrites) throw IOException("write failed")
      return delegate.updateData(transform)
    }
  }
}
