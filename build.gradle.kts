// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
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
             * All rights reserved.
             */
            """.trimIndent(),
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
