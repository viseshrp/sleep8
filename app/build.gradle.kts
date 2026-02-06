import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    id("de.mannodermaus.android-junit5")
    id("org.jetbrains.kotlin.plugin.compose")
    id("jacoco")
}

android {
    namespace = "com.sleep8"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.sleep8"
        minSdk = 31
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        debug {
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests.all {
            it.useJUnitPlatform()
        }
        unitTests.isIncludeAndroidResources = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
}

jacoco {
    toolVersion = "0.8.11"
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("com.google.android.material:material:1.12.0")

    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.51.1")
    ksp("com.google.dagger:hilt-compiler:2.51.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.3")
    testImplementation("org.junit.vintage:junit-vintage-engine:5.11.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.11.3")
    testImplementation("io.mockk:mockk:1.13.13")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("androidx.test:core:1.6.1")
    testImplementation("org.robolectric:robolectric:4.14.1")

    androidTestImplementation(platform("androidx.compose:compose-bom:2024.12.01"))
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("androidx.room:room-testing:2.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("io.mockk:mockk-android:1.13.13")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

val jacocoExcludes = listOf(
    "**/R.class",
    "**/R$*.class",
    "**/BuildConfig.*",
    "**/Manifest*.*",
    "**/*Test*.*",
    "**/Hilt_*.*",
    "**/*_Factory*.*",
    "**/*_HiltModules*.*",
    "**/*_MembersInjector*.*"
)

val criticalCoverageIncludes = listOf(
    // Core automation engine: state transitions, window validation, and duration/time logic.
    "com/sleep8/domain/manager/ArmManager*",
    "com/sleep8/domain/manager/StateMachineManager*",
    "com/sleep8/domain/state/StateHolder*",
    "com/sleep8/domain/overlay/AlarmOverlayPolicy*",
    "com/sleep8/domain/validator/NightWindowValidator*",
    "com/sleep8/util/AlarmDurationValidator*",
    "com/sleep8/util/TimeUtils*"
)

fun criticalClassDirectories() = files(
    fileTree(layout.buildDirectory.dir("intermediates/javac/debug/classes")) {
        include(criticalCoverageIncludes)
        exclude(jacocoExcludes)
    },
    fileTree(layout.buildDirectory.dir("tmp/kotlin-classes/debug")) {
        include(criticalCoverageIncludes)
        exclude(jacocoExcludes)
    }
)

fun debugExecutionData() = fileTree(layout.buildDirectory) {
    include("outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec")
    include("jacoco/testDebugUnitTest.exec")
}

tasks.register<JacocoReport>("jacocoCriticalReport") {
    dependsOn("testDebugUnitTest")

    reports {
        xml.required.set(true)
        html.required.set(true)
        xml.outputLocation.set(layout.buildDirectory.file("reports/jacoco/jacocoTestReport/jacocoTestReport.xml"))
        html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/jacocoTestReport/html"))
    }

    classDirectories.setFrom(criticalClassDirectories())
    sourceDirectories.setFrom(files("src/main/java", "src/main/kotlin"))
    executionData.setFrom(debugExecutionData())
}

tasks.register<JacocoCoverageVerification>("jacocoCriticalCoverageVerification") {
    dependsOn("jacocoCriticalReport")

    classDirectories.setFrom(criticalClassDirectories())
    sourceDirectories.setFrom(files("src/main/java", "src/main/kotlin"))
    executionData.setFrom(debugExecutionData())

    violationRules {
        rule {
            limit {
                minimum = "0.89".toBigDecimal()
            }
        }
    }
}

tasks.register("jacocoTestReport") {
    dependsOn("jacocoCriticalReport")
}

tasks.register("jacocoTestCoverageVerification") {
    dependsOn("jacocoCriticalCoverageVerification")
}
