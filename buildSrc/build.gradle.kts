import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
//    id("gradle.plugin.com.craigburke.gradle:karma-gradle") version "1.4.4"
//    id("com.moowork.node") version "1.2.0"
}


dependencies {
//    compile("org.jetbrains.kotlin:kotlin-gradle-plugin:1.3.21")
//    compile("com.moowork.gradle:gradle-node-plugin:1.2.0")
}

repositories {
    jcenter()
    mavenCentral()
    maven(url = "https://plugins.gradle.org/m2/")

}
//apply(plugin = "com.moowork.node")

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