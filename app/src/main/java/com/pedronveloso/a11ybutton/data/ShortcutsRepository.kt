/*
 * Copyright (C) 2026 Pedro Veloso
 * SPDX-License-Identifier: Apache-2.0
 */
package com.pedronveloso.a11ybutton.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.res.Resources
import android.content.res.XmlResourceParser
import androidx.core.content.IntentCompat
import androidx.core.net.toUri
import com.pedronveloso.a11ybutton.model.AppShortcutGroup
import com.pedronveloso.a11ybutton.model.InstalledApp
import com.pedronveloso.a11ybutton.model.ShortcutEntry
import com.pedronveloso.a11ybutton.model.ShortcutTarget
import com.pedronveloso.a11ybutton.service.LaunchIntentFactory
import java.text.Collator
import org.xmlpull.v1.XmlPullParser
import timber.log.Timber

/**
 * Finds app shortcuts the button can launch.
 *
 * Android only lets the default launcher enumerate shortcuts, so this reads the two sources any app
 * can reach: static shortcuts declared in other apps' manifests, and activities that handle
 * [Intent.ACTION_CREATE_SHORTCUT]. Dynamic and pinned shortcuts are not discoverable.
 */
class ShortcutsRepository(
    context: Context,
    private val installedAppsRepository: InstalledAppsRepository,
) {
  private val packageManager = context.packageManager

  fun getShortcutGroups(): List<AppShortcutGroup> {
    val entries = buildList {
      installedAppsRepository.getLaunchableApps().forEach { app ->
        staticShortcutsFor(app).forEach { add(app to it) }
      }
      createShortcutEntries().forEach { add(it) }
    }
    return buildShortcutGroups(entries).also { Timber.d("Loaded shortcuts for %s apps", it.size) }
  }

  private fun staticShortcutsFor(app: InstalledApp): List<ShortcutEntry> =
      try {
        val component = ComponentName.unflattenFromString(app.componentName)
        val activityInfo =
            component?.let { packageManager.getActivityInfo(it, PackageManager.GET_META_DATA) }
        val resourceId = activityInfo?.metaData?.getInt(SHORTCUTS_META_DATA, 0) ?: 0
        if (activityInfo == null || resourceId == 0) {
          emptyList()
        } else {
          val resources = packageManager.getResourcesForApplication(activityInfo.applicationInfo)
          packageManager
              .getXml(app.packageName, resourceId, activityInfo.applicationInfo)
              ?.use { parser -> parseStaticShortcuts(parser, resources, app.packageName) }
              .orEmpty()
        }
      } catch (exception: Exception) {
        // One app with a broken or unreadable shortcuts file must not hide everyone else's.
        Timber.w(exception, "Could not read static shortcuts for %s", app.packageName)
        emptyList()
      }

  private fun parseStaticShortcuts(
      parser: XmlResourceParser,
      resources: Resources,
      packageName: String,
  ): List<ShortcutEntry> {
    val result = mutableListOf<ShortcutEntry>()
    var draft: ShortcutDraft? = null
    var event = parser.eventType
    while (event != XmlPullParser.END_DOCUMENT) {
      when (event) {
        XmlPullParser.START_TAG ->
            when (parser.name) {
              "shortcut" ->
                  draft =
                      ShortcutDraft(
                          id = parser.getAttributeValue(ANDROID_NS, "shortcutId"),
                          enabled = parser.getAttributeBooleanValue(ANDROID_NS, "enabled", true),
                          label =
                              parser.resolveString(resources, "shortcutShortLabel")
                                  ?: parser.resolveString(resources, "shortcutLongLabel"),
                      )
              "intent" -> draft?.let { it.intent = parser.readIntent() }
              "categories" ->
                  parser.getAttributeValue(ANDROID_NS, "name")?.let { name ->
                    draft?.intent?.addCategory(name)
                  }
              "extra" -> draft?.hasExtras = true
            }
        XmlPullParser.END_TAG ->
            if (parser.name == "shortcut") {
              draft?.toEntry(packageName)?.let(result::add)
              draft = null
            }
      }
      event = parser.next()
    }
    return result
  }

  private fun createShortcutEntries(): List<Pair<InstalledApp, ShortcutEntry>> =
      packageManager.queryIntentActivities(Intent(Intent.ACTION_CREATE_SHORTCUT), 0).mapNotNull {
          resolveInfo ->
        resolveInfo.toCreatorPair()
      }

  private fun ResolveInfo.toCreatorPair(): Pair<InstalledApp, ShortcutEntry>? {
    val info = activityInfo ?: return null
    val component = ComponentName(info.packageName, info.name).flattenToString()
    val appLabel = info.applicationInfo.loadLabel(packageManager).toString()
    val label = loadLabel(packageManager).toString().ifBlank { appLabel }
    return InstalledApp(
        packageName = info.packageName,
        componentName = component,
        label = appLabel,
    ) to
        ShortcutEntry.Creator(
            packageName = info.packageName,
            componentName = component,
            label = label,
        )
  }

  /** Turns the result of an app's create-shortcut screen into a saveable target, or null. */
  // The create-shortcut protocol still returns its result through these legacy extras.
  @Suppress("DEPRECATION")
  fun shortcutFromCreateResult(
      packageName: String,
      fallbackLabel: String,
      result: Intent?,
  ): ShortcutTarget? {
    result ?: return null
    val shortcutIntent =
        IntentCompat.getParcelableExtra(result, Intent.EXTRA_SHORTCUT_INTENT, Intent::class.java)
            ?: return null
    val sanitized =
        LaunchIntentFactory.createShortcutIntent(
            intentUri = shortcutIntent.toUri(Intent.URI_INTENT_SCHEME),
            expectedPackage = packageName,
        ) ?: return null
    if (sanitized.resolveActivity(packageManager) == null) return null
    val label =
        result.getStringExtra(Intent.EXTRA_SHORTCUT_NAME).orEmpty().ifBlank { fallbackLabel }
    return ShortcutTarget(
        packageName = packageName,
        shortcutId = null,
        label = label,
        intentUri = sanitized.toUri(Intent.URI_INTENT_SCHEME),
    )
  }

  private inner class ShortcutDraft(
      val id: String?,
      val enabled: Boolean,
      val label: String?,
  ) {
    var intent: Intent? = null
    var hasExtras = false

    fun toEntry(packageName: String): ShortcutEntry? {
      val intent = intent ?: return null
      val label = label?.takeIf { it.isNotBlank() } ?: return null
      // Extras are skipped: the XML does not say whether a value is a string, int or boolean, and
      // launching with a wrongly typed extra would misbehave quietly.
      if (!enabled || hasExtras) return null
      val sanitized =
          LaunchIntentFactory.createShortcutIntent(
              intentUri = intent.toUri(Intent.URI_INTENT_SCHEME),
              expectedPackage = packageName,
          ) ?: return null
      if (intent.component == null && intent.`package` == null) return null
      // Not exported (or otherwise unreachable from here) means the launch would fail later.
      if (sanitized.resolveActivity(packageManager) == null) return null
      return ShortcutEntry.Ready(
          ShortcutTarget(
              packageName = packageName,
              shortcutId = id,
              label = label,
              intentUri = sanitized.toUri(Intent.URI_INTENT_SCHEME),
          ),
      )
    }
  }

  private companion object {
    const val SHORTCUTS_META_DATA = "android.app.shortcuts"
    const val ANDROID_NS = "http://schemas.android.com/apk/res/android"

    fun XmlResourceParser.resolveString(resources: Resources, attribute: String): String? {
      val resourceId = getAttributeResourceValue(ANDROID_NS, attribute, 0)
      return if (resourceId != 0) {
        resources.getString(resourceId)
      } else {
        getAttributeValue(ANDROID_NS, attribute)
      }
    }

    fun XmlResourceParser.readIntent(): Intent {
      val intent = Intent(getAttributeValue(ANDROID_NS, "action"))
      getAttributeValue(ANDROID_NS, "data")?.let { intent.data = it.toUri() }
      val targetPackage = getAttributeValue(ANDROID_NS, "targetPackage")
      val targetClass = getAttributeValue(ANDROID_NS, "targetClass")
      if (targetPackage != null && targetClass != null) {
        intent.component = ComponentName(targetPackage, targetClass)
      } else if (targetPackage != null) {
        intent.`package` = targetPackage
      }
      return intent
    }
  }
}

/** Groups entries by app, apps A-Z, entries A-Z within an app; apps with no entries are dropped. */
internal fun buildShortcutGroups(
    entries: List<Pair<InstalledApp, ShortcutEntry>>,
    collator: Collator = Collator.getInstance(),
): List<AppShortcutGroup> =
    entries
        .groupBy { (app, _) -> app.packageName }
        .values
        .map { pairs ->
          AppShortcutGroup(
              app = pairs.first().first,
              entries =
                  pairs
                      .map { it.second }
                      .distinct()
                      .sortedWith(compareBy(collator) { it.label.lowercase() }),
          )
        }
        .sortedWith(compareBy(collator) { it.app.label.lowercase() })

/**
 * Case-insensitive search. Matching the app's name or package keeps all its shortcuts; otherwise
 * only shortcuts whose own label matches are kept.
 */
internal fun List<AppShortcutGroup>.filterByQuery(query: String): List<AppShortcutGroup> {
  val needle = query.trim()
  if (needle.isEmpty()) return this
  return mapNotNull { group ->
    if (
        group.app.label.contains(needle, ignoreCase = true) ||
            group.app.packageName.contains(needle, ignoreCase = true)
    ) {
      group
    } else {
      group.entries
          .filter { it.label.contains(needle, ignoreCase = true) }
          .takeIf { it.isNotEmpty() }
          ?.let { group.copy(entries = it) }
    }
  }
}
