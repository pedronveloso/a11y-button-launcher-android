// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.kotlin.compose) apply false
  alias(libs.plugins.spotless)
}

spotless {
  kotlin {
    target("**/*.kt")
    targetExclude("**/build/**")
    licenseHeader(
        """
        /*
         * Copyright (C) ${'$'}YEAR Pedro Veloso
         * SPDX-License-Identifier: Apache-2.0
         */
        """
            .trimIndent(),
        "^(package|import|@file:)",
    )
    ktfmt()
    trimTrailingWhitespace()
    endWithNewline()
  }

  kotlinGradle {
    target("**/*.gradle.kts")
    ktfmt()
    trimTrailingWhitespace()
    endWithNewline()
  }

  format("misc") {
    target("**/*.gradle", "**/*.md", "**/.gitignore")
    trimTrailingWhitespace()
    leadingTabsToSpaces(2)
  }
}
