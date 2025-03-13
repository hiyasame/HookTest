plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.example.javahook")
}

javahook {
//    hook {
//        method {
//            owner = "java/lang/Math"
//            name = "random"
//            descriptor = "()D"
//            isStatic = true
//        }
//
//        replaceWith {
//            owner = "com/example/hooktest/JavaHookTest"
//            name = "hookRandom"
//            descriptor = "()D"
//        }
//    }
}

android {
    namespace = "com.example.hooktest"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.hooktest"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    packaging {
        jniLibs.useLegacyPackaging = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {

    implementation(project(":nativelib"))
    implementation(project(":javahook_api"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}