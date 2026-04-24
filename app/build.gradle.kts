import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
}

android {
  namespace = "com.pedronveloso.a11ybutton"
  compileSdk { version = release(37) }

  defaultConfig {
    applicationId = "com.pedronveloso.a11ybutton"
    minSdk = 30
    targetSdk = 37
    versionCode = 1
    versionName = "1.0.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(
          getDefaultProguardFile("proguard-android-optimize.txt"),
          "proguard-rules.pro",
      )
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  kotlin { compilerOptions { jvmTarget = JvmTarget.JVM_17 } }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  lint {
    warningsAsErrors = true
    baseline = file("lint-baseline.xml")
    disable += "GradleDependency"
    disable += "ExpiredTargetSdkVersion"
    disable += "OldTargetApi"
  }
}

dependencies {
  lintChecks(libs.slack.compose.lints)

  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.datastore.preferences)
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.timber)
  implementation(libs.androidx.work.runtime.ktx)

  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)

  debugImplementation(libs.androidx.compose.ui.tooling)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
}
