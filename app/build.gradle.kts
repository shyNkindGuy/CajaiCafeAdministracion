import java.util.concurrent.TimeUnit;
import org.gradle.api.GradleException;
plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

//funcion para obtener conteno de commits

fun getGitcommit(): Int{
    return try{
        val process = ProcessBuilder("git", "rev-list", "--count","HEAD")
            .redirectErrorStream(true)
            .start()
        process.waitFor(5, TimeUnit.SECONDS)
        val output = process.inputStream.bufferedReader().readText();

        val count = output.trim().toInt()
        if (count < 1) 1 else count
    }catch (e: Exception){
        1
    }
}

val commitCount = getGitcommit()

android {
    namespace = "com.example.cajaicafeadministracion"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.cajaicafeadministracion"
        minSdk = 24
        targetSdk = 35
        versionCode = commitCount
        versionName = "1.0.$commitCount"//se usa la varible para automatizarlo

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
}

dependencies {
    implementation(platform("com.google.firebase:firebase-bom:34.1.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-database")
    implementation("com.google.firebase:firebase-auth")
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}