import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("kotlin-multiplatform") version Deps.kotlinMultiPlatformVersion
}
repositories {
    mavenCentral()
}

apply {
    plugin("kotlin-dce-js")
}


kotlin {
    val jvm = presets["jvmWithJava"].createTarget("jvm")
    val js = presets["js"].createTarget("js")

    // thanks to this `gradle check` will find Java classes within this Kotlin project.
    // We need those because the unit tests are testing the serializer serializing Java objects
    // (written in Java to be sure that Kotlin doesn't mess anything during compilation).
    val javaConvention = jvm.project.convention.getPlugin(JavaPluginConvention::class.java)
    javaConvention.sourceSets.getByName("test").java.srcDir("src/jvmTest/java")
    javaConvention.sourceSets.getByName("test").java.setOutputDir(File("build/classes/kotlin/jvm/test"))

    targets.add(jvm)
    targets.add(js)


    sourceSets {
        commonMain {
            dependencies {
                implementation(kotlin("stdlib-common"))
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(kotlin("stdlib-jdk7"))
                implementation(kotlin("reflect"))
                api("net.onedaybeard.artemis:artemis-odb:${Deps.artemisOdbVersion}")
                api("org.webbitserver:webbit:${Deps.webbitVersion}")
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlin("test-junit"))
            }
        }
        val jsMain by getting {
            dependencies {
                implementation(kotlin("stdlib-js"))
            }
        }
        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
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
        }
    }

    named<KotlinCompile>("compileTestKotlinJvm") {
        kotlinOptions {
            jvmTarget = "1.8"
        }
    }

    // thanks to this we don't have to delete `build/test-results` folder when running
    // `gradle check` instead of `gradle cleanTest check` which is hard to remember.
    val cleanTest = named("cleanTest")
    named("check") {
        dependsOn(cleanTest)
    }
}
