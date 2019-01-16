import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
}
repositories {
    mavenCentral()
}

subprojects {
    allprojects {
        java {
            sourceCompatibility = JavaVersion.VERSION_1_6
            targetCompatibility = JavaVersion.VERSION_1_6
        }
    }
}

tasks.withType(KotlinCompile::class.java).all {
    kotlinOptions {
        jvmTarget = "1.6"
        noReflect = true
        noStdlib = true
        sourceCompatibility = "1.6"
        targetCompatibility = "1.6"
    }
}