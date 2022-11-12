import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.20"
    application
}

group = "me.gefro"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("dev.inmo:tgbotapi:3.3.0")
    implementation ("com.squareup.retrofit2:retrofit:$2.0.9")
    implementation ("com.squareup.retrofit2:converter-moshi:$2.0.9")
    implementation ("com.squareup.okhttp3:okhttp:$4.0.9")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

application {
    mainClass.set("MainKt")
}