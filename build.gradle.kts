import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("kotlin-multiplatform") version Deps.kotlinMultiPlatformVersion
}
repositories {
    mavenCentral()
}

kotlin {
    val jvm = presets["jvm"].createTarget("jvm")
    val js = presets["js"].createTarget("js")

    targets.add(jvm)
    targets.add(js)


    sourceSets {
        commonMain {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-stdlib-common")
            }
        }
        commonTest {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-test-common")
                implementation("org.jetbrains.kotlin:kotlin-test-annotations-common")
            }
        }
        get("jvmMain").apply {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7")
                implementation("org.jetbrains.kotlin:kotlin-reflect")
                api("net.onedaybeard.artemis:artemis-odb:${Deps.artemisOdbVersion}")
                api("org.webbitserver:webbit:${Deps.webbitVersion}")
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
    val compileKotlinJs = named<Kotlin2JsCompile>("compileKotlinJs")

    val assembleWeb = task<Copy>("assembleWeb") {
        from("src/jsMain/web/index.html")
        into("build/classes/kotlin/js/main")

        dependsOn(compileKotlinJs)
    }

    compileKotlinJs {
        kotlinOptions {
            sourceMap = true
            sourceMapEmbedSources = "always"
        }

        finalizedBy(assembleWeb)
    }

    withType<KotlinCompile>().configureEach {
        kotlinOptions {
            jvmTarget = "1.6"
            noReflect = true
            noStdlib = true
        }
    }

    named<KotlinCompile>("compileTestKotlinJvm") {
        kotlinOptions {
            jvmTarget = "1.8"
        }
    }
}
