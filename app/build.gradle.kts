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
    versionCode = 2
    versionName = "Beta 2"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  buildTypes {
    release {
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(
          getDefaultProguardFile("proguard-android-optimize.txt"),
          "proguard-rules.pro",
      )
    }
  }
  kotlin { jvmToolchain(21) }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
  }

  // Exclude the dependencies info files from the final APK and Bundle, for F-Droid.
  dependenciesInfo {
    includeInApk = false
    includeInBundle = false
  }

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
