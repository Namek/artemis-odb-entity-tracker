import org.jetbrains.kotlin.gradle.dsl.KotlinJsCompile
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsTargetPreset
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinOnlyTarget
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile

plugins {
    id("kotlin-multiplatform") version "1.3.11"
}
repositories {
    mavenCentral()
}

kotlin {
    val jvm = presets["jvm"].createTarget("jvm")
    val js = presets["js"].createTarget("js")

    targets.add(jvm)
    targets.add(js)

    sourceSets.apply {
        get("commonMain").apply {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-stdlib-common")
            }
        }
        get("commonTest").apply {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-test-common")
                implementation("org.jetbrains.kotlin:kotlin-test-annotations-common")
            }
        }
        get("jvmMain").apply {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
                implementation("org.jetbrains.kotlin:kotlin-reflect")
                api("net.onedaybeard.artemis:artemis-odb:2.1.0")
                api("org.webbitserver:webbit:0.4.15")
            }
        }
        get("jvmTest").apply {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-test")
                implementation("org.jetbrains.kotlin:kotlin-test-junit")
            }
        }
        get("jsMain").apply {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-stdlib-js")
            }

        }
        get("jsTest").apply {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-test-js")
            }
        }


    }
}

tasks {
    named<Kotlin2JsCompile>("compileKotlinJs") {
        kotlinOptions {
            sourceMap = true
            sourceMapEmbedSources = "always"
        }
    }
}