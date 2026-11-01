import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

// ============================================================================
//  ADMOB IDS: THE ONLY PLACE TO EDIT. Currently Google's TEST IDs for BOTH builds.
//  Replace the RELEASE values with SafeSpot's real AdMob IDs before publishing.
// ============================================================================
val admobTestAppId = "ca-app-pub-3940256099942544~3347511713"
val admobTestBanner = "ca-app-pub-3940256099942544/9214589741"
val admobTestInterstitial = "ca-app-pub-3940256099942544/1033173712"
val admobTestRewarded = "ca-app-pub-3940256099942544/5224354917"
val admobTestNative = "ca-app-pub-3940256099942544/2247696110"
val admobTestAppOpen = "ca-app-pub-3940256099942544/9257395921"

val admobReleaseAppId = admobTestAppId          // TODO real
val admobReleaseBanner = admobTestBanner        // TODO real
val admobReleaseInterstitial = admobTestInterstitial // TODO real
val admobReleaseRewarded = admobTestRewarded    // TODO real
val admobReleaseNative = admobTestNative        // TODO real
val admobReleaseAppOpen = admobTestAppOpen      // TODO real
// ============================================================================

val keystorePropsFile = rootProject.file("keystore/keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) keystorePropsFile.inputStream().use { load(it) }
}

android {
    namespace = "com.safespot.scanner"
    compileSdk {
        version = release(37) { minorApiLevel = 1 }
    }

    defaultConfig {
        applicationId = "com.safespot.scanner"
        minSdk = 24
        targetSdk = 36
        versionCode = 11
        versionName = "1.2.5"
        ndk { abiFilters += ((project.findProperty("abis") as String?) ?: "arm64-v8a,armeabi-v7a,x86_64").split(",") }
        buildConfigField("String", "SERVERS_URL", "\"https://micky9889900.github.io/vpn/servers.json\"")
        buildConfigField("String", "MB_AUTH_KEY", "\"${(project.findProperty("mbAuthKey") ?: "")}\"")
        buildConfigField("String", "VPNGATE_URL", "\"https://www.vpngate.net/api/iphone/\"")
    }

    flavorDimensions += listOf("implementation", "ovpnimpl")
    productFlavors {
        create("skeleton") { dimension = "implementation" }
        create("ovpn2") { dimension = "ovpnimpl" }
    }

    signingConfigs {
        create("release") {
            if (keystorePropsFile.exists()) {
                storeFile = rootProject.file("keystore/" + keystoreProps["storeFile"])
                storePassword = keystoreProps["storePassword"] as String
                keyAlias = keystoreProps["keyAlias"] as String
                keyPassword = keystoreProps["keyPassword"] as String
            }
        }
    }

    buildFeatures { compose = true; buildConfig = true; resValues = true }

    buildTypes {
        debug {
            resValue("string", "admob_app_id", admobTestAppId)
            buildConfigField("String", "AD_BANNER_ID", "\"$admobTestBanner\"")
            buildConfigField("String", "AD_INTERSTITIAL_ID", "\"$admobTestInterstitial\"")
            buildConfigField("String", "AD_REWARDED_ID", "\"$admobTestRewarded\"")
            buildConfigField("String", "AD_NATIVE_ID", "\"$admobTestNative\"")
            buildConfigField("String", "AD_APP_OPEN_ID", "\"$admobTestAppOpen\"")
        }
        release {
            resValue("string", "admob_app_id", admobReleaseAppId)
            buildConfigField("String", "AD_BANNER_ID", "\"$admobReleaseBanner\"")
            buildConfigField("String", "AD_INTERSTITIAL_ID", "\"$admobReleaseInterstitial\"")
            buildConfigField("String", "AD_REWARDED_ID", "\"$admobReleaseRewarded\"")
            buildConfigField("String", "AD_NATIVE_ID", "\"$admobReleaseNative\"")
            buildConfigField("String", "AD_APP_OPEN_ID", "\"$admobReleaseAppOpen\"")
            // R8 code shrinking. Resource shrinking stays OFF: UI art is looked up by name (getIdentifier).
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (keystorePropsFile.exists()) signingConfig = signingConfigs.getByName("release")
        }
    }

    ndkVersion = "30.0.14904198"
    packaging { jniLibs { useLegacyPackaging = true; excludes += "**/libovpn3.so" } }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    testOptions { unitTests { isIncludeAndroidResources = true; isReturnDefaultValues = true } }
    lint { abortOnError = false; checkReleaseBuilds = false }
}

dependencies {
    implementation(project(":openvpn"))
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.documentfile:documentfile:1.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    val bom = platform("androidx.compose:compose-bom:2026.09.00")
    implementation(bom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    implementation("com.google.android.gms:play-services-ads:25.4.0")
    implementation("com.google.android.ump:user-messaging-platform:3.2.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.16")
    testImplementation("androidx.test:core:1.7.0")
}
