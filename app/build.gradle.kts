plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.researchproject"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.researchproject"
        minSdk = 24
        targetSdk = 35
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(libs.google.scanner)
    implementation(libs.play.services)
    implementation(libs.play.services.location)
    implementation(libs.commons.math3)
    implementation("de.hdodenhof:circleimageview:3.1.0")
    implementation ("jp.wasabeef:blurry:4.0.0")
    implementation("com.google.android.material:material:1.9.0")
    implementation ("org.apache.commons:commons-math3:3.6.1")

}