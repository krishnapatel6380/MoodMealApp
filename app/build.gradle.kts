plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.krishna.moodmeal"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.krishna.moodmeal"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(platform("com.google.firebase:firebase-bom:33.16.0"))
    implementation("com.google.firebase:firebase-analytics")

    // Firebase Realtime Database
    implementation("com.google.firebase:firebase-database-ktx")
// Firebase Firestore
    implementation("com.google.firebase:firebase-firestore-ktx")
// Material Design Components
    implementation("com.google.android.material:material:1.9.0")

    implementation("androidx.coordinatorlayout:coordinatorlayout:1.2.0")
// CoordinatorLayout
    implementation("androidx.recyclerview:recyclerview:1.3.0")
// RecyclerView
    implementation("androidx.cardview:cardview:1.0.0")
}