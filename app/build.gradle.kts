import java.util.Properties

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.stellaguard"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.stellaguard"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        manifestPlaceholders["MAPS_API_KEY"] = localProperties.getProperty("MAPS_API_KEY") ?: ""
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
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {

    // Koristimo stariji, ali stabilan BOM za Compose
    implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    // Uklanjamo kotlin-bom, jer BOM za compose to rešava

    // Vraćamo core-ktx na verziju kompatibilnu sa SDK 34
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    // --- Naše dodate zavisnosti, usklađujemo ih ---
    implementation("com.google.firebase:firebase-auth-ktx") // Bez verzije, koristiće BOM
    implementation("com.google.firebase:firebase-firestore-ktx") // Bez verzije, koristiće BOM
    implementation("com.google.firebase:firebase-storage-ktx") // Bez verzije, koristiće BOM

    // Dodajemo Firebase BOM da upravlja verzijama
    implementation(platform("com.google.firebase:firebase-bom:33.0.0"))

    // Vraćamo navigaciju na verziju koja radi sa starim Compose-om
    implementation("androidx.navigation:navigation-compose:2.7.7")

    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Google Maps biblioteke koje smo dodali
    implementation("com.google.maps.android:maps-compose:4.3.3")
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.gms:play-services-location:21.2.0")

    // Test zavisnosti (ostaju iste)
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.10.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    implementation("com.google.accompanist:accompanist-permissions:0.31.5-beta")
    // ovo pisem za treci zahtev
    implementation("com.google.firebase:firebase-messaging-ktx")
    implementation("androidx.compose.material:material-icons-extended")


}