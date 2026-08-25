plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
}

android {
  namespace = "com.example"
  compileSdk = 36

  defaultConfig {
    applicationId = "com.example.pulseemulator"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"
    
    ndk {
        abiFilters += listOf("arm64-v8a", "x86_64")
    }
    
    externalNativeBuild {
        cmake {
            arguments += listOf(
                "-DANDROID_STL=c++_shared",
                "-DCMAKE_BUILD_TYPE=Release"
            )
            cppFlags += listOf("-std=c++17", "-fexceptions")
        }
    }
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }
  
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  
  buildFeatures {
    compose = true
  }
  
  externalNativeBuild {
    cmake {
      path = file("CMakeLists.txt")
      version = "3.22.1"
    }
  }
}

dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
}
