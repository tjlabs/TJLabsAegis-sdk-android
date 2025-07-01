import com.android.build.gradle.internal.api.BaseVariantOutputImpl
import java.util.Properties

plugins {
//    id("com.android.application")
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
}

val versionMajor = 1
val versionMinor = 0
val versionPatch = 3

// 최상단에 import 추가

// local.properties 파일 읽기
val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        load(localPropertiesFile.inputStream())
    }
}

val tjGroupId: String = localProperties.getProperty("GROUP_ID") ?: ""
val tjArtifactId: String = localProperties.getProperty("ARTIFACT_ID") ?: ""


android {
    namespace = "com.tjlabs.tjlabsaegis_sdk_android"
    compileSdk = 35

    defaultConfig {
//        applicationId = "com.tjlabs.tjlabsaegis_sdk_android"
//        versionCode = versionMajor * 1000 + versionMinor * 100 + versionPatch
//        versionName = "$versionMajor.$versionMinor.$versionPatch"
        minSdk = 29
        targetSdk = 34
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "TJLABS_SDK_VERSION", "\"$versionMajor.$versionMinor.$versionPatch\"")

    }

    libraryVariants.all {
        outputs.all {
            (this as BaseVariantOutputImpl).outputFileName = "app-release-aegis.aar"
        }
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
}

dependencies {
    implementation ("com.tjlabs:appauthlib:1.0.3")

    implementation ("androidx.security:security-crypto-ktx:1.1.0-alpha03")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")


    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.room.compiler)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                groupId = tjGroupId
                artifactId = tjArtifactId
                version = "$versionMajor.$versionMinor.$versionPatch"
            }
        }
    }
}
