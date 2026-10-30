import java.security.MessageDigest
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.baselineprofile)
}

baselineProfile {
    // One profile for both store flavors, kept in src/main.
    mergeIntoMain = true
}

composeCompiler {
    stabilityConfigurationFiles.add(rootProject.layout.projectDirectory.file("app/compose_stability.conf"))
}

android {
    namespace = "com.ekquickfind.app"
    compileSdk =
        libs.versions.compileSdk
            .get()
            .toInt()

    defaultConfig {
        applicationId = "com.ekquickfind.app"
        minSdk =
            libs.versions.minSdk
                .get()
                .toInt()
        targetSdk =
            libs.versions.targetSdk
                .get()
                .toInt()
        versionCode = providers.gradleProperty("ekquickfind.versionCode").get().toInt()
        versionName = providers.gradleProperty("ekquickfind.versionName").get()
        testInstrumentationRunner = "com.ekquickfind.app.HiltTestRunner"
        buildConfigField(
            "String",
            "PROJECT_PAGE",
            "\"github.com/${providers.gradleProperty("ekquickfind.githubRepo").get()}\"",
        )
        buildConfigField("String", "MIN_ANDROID", "\"${libs.versions.minAndroid.get()}\"")
    }

    val githubRepo = providers.gradleProperty("ekquickfind.githubRepo").get()
    // Default feed: the latest GitHub release of this repository.
    val updateFeedUrl =
        providers
            .gradleProperty("ekquickfind.updateFeedUrl")
            .getOrElse("")
            .ifBlank { "https://github.com/$githubRepo/releases/latest/download/update.json" }
            .takeUnless { it == "off" }
            .orEmpty()
    flavorDimensions += "distribution"
    productFlavors {
        // For app stores that deliver updates themselves: no update feed and no install permission.
        create("store") {
            dimension = "distribution"
            buildConfigField("String", "UPDATE_FEED_URL", "\"\"")
        }
        // Direct download (GitHub Releases): checks the feed daily and installs its own updates.
        create("direct") {
            dimension = "distribution"
            isDefault = true
            // One universal APK for sideloading: nearly every Android 13+ phone is 64-bit ARM, so skip the other
            // processors' native code (ML Kit's is ~10 MB each). The store build keeps every processor.
            ndk { abiFilters += "arm64-v8a" }
            buildConfigField("String", "UPDATE_FEED_URL", "\"$updateFeedUrl\"")
        }
    }

    // Compressed native code: a much smaller download (ML Kit's translator alone is ~16 MB), unpacked on install.
    packaging { jniLibs { useLegacyPackaging = true } }

    // Release key lives outside the project; keystore.properties (not committed) points at it.
    val keystoreProps =
        rootProject.file("keystore.properties").takeIf { it.exists() }?.let { file ->
            Properties().apply { file.inputStream().use(::load) }
        }
    signingConfigs {
        if (keystoreProps != null) {
            create("release") {
                storeFile = file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Without a release key the build stays installable locally with the debug key.
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
        // Robolectric pokes at FileDescriptor internals on JDK 21.
        unitTests.all { it.jvmArgs("--add-opens=java.base/java.io=ALL-UNNAMED", "--add-exports=java.base/jdk.internal.access=ALL-UNNAMED") }
    }
}

dependencies {
    implementation(project(":core:search"))
    implementation(project(":providers"))
    implementation(project(":data"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.window)
    implementation(libs.androidx.profileinstaller)
    implementation(libs.androidx.tracing)
    baselineProfile(project(":benchmark"))

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.material3.window.size)
    implementation(libs.compose.material.icons)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.coil.compose)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.work)
    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)

    testImplementation(libs.junit4)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.glance.appwidget.testing)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)

    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.uiautomator)
    androidTestImplementation(libs.androidx.test.espresso)
    androidTestImplementation(libs.androidx.test.espresso.intents)
    androidTestImplementation(libs.hilt.android.testing)
    androidTestImplementation(libs.room.runtime)
    kspAndroidTest(libs.hilt.compiler)
    androidTestImplementation(libs.kotlinx.coroutines.test)
}

/** Copies a built APK to build/outputs/update/ and writes the update.json the app checks. */
abstract class UpdateFeedTask : DefaultTask() {
    @get:InputDirectory
    abstract val apkDir: DirectoryProperty

    @get:Input
    abstract val versionCode: Property<Int>

    @get:Input
    abstract val versionName: Property<String>

    @get:Input
    @get:Optional
    abstract val notes: Property<String>

    @get:OutputDirectory
    abstract val outDir: DirectoryProperty

    @TaskAction
    fun write() {
        val apk =
            apkDir
                .get()
                .asFile
                .listFiles { f -> f.extension == "apk" }
                ?.singleOrNull()
                ?: throw GradleException("Expected one APK in ${apkDir.get().asFile}")
        val out =
            outDir.get().asFile.apply {
                deleteRecursively()
                mkdirs()
            }
        val name = "ekQuickFind-${versionCode.get()}.apk"
        val copy = apk.copyTo(File(out, name), overwrite = true)
        val sha =
            MessageDigest
                .getInstance("SHA-256")
                .digest(copy.readBytes())
                .joinToString("") { "%02x".format(it) }

        fun quote(s: String) = "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
        val notesJson = notes.orNull?.let { ",\n  \"notes\": ${quote(it)}" }.orEmpty()
        File(out, "update.json").writeText(
            """
            |{
            |  "versionCode": ${versionCode.get()},
            |  "versionName": ${quote(versionName.get())},
            |  "apk": ${quote(name)},
            |  "sha256": ${quote(sha)},
            |  "size": ${copy.length()}$notesJson
            |}
            """.trimMargin() + "\n",
        )
        logger.lifecycle("Update feed written to $out (upload both files to the folder update.json is served from)")
    }
}

// updateFeedRelease for real releases; updateFeedDebug for testing against a debug install.
// Only the direct-download build updates itself.
listOf("directDebug", "directRelease").forEach { variant ->
    val cap = variant.replaceFirstChar { it.uppercase() }
    tasks.register<UpdateFeedTask>("updateFeed$cap") {
        group = "ekquickfind"
        description = "Builds the $variant APK and writes update.json next to it"
        dependsOn("assemble$cap")
        apkDir.set(layout.buildDirectory.dir("outputs/apk/direct/${variant.removePrefix("direct").lowercase()}"))
        versionCode.set(providers.gradleProperty("ekquickfind.versionCode").map(String::toInt))
        versionName.set(providers.gradleProperty("ekquickfind.versionName"))
        notes.set(providers.gradleProperty("ekquickfind.releaseNotes"))
        outDir.set(layout.buildDirectory.dir("outputs/update/$variant"))
    }
}
