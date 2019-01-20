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

    // thanks to this `gradle test` will found Java classes within this Kotlin project.
    // We need those because the unit tests are testing Java serializer.
    val javaConvention = jvm.project.convention.getPlugin(JavaPluginConvention::class.java)
    javaConvention.sourceSets.getByName("test").java.srcDir("src/jvmTest/kotlin")

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
        }
    }

    named<KotlinCompile>("compileTestKotlinJvm") {
        kotlinOptions {
            jvmTarget = "1.8"
        }
    }

    // thanks to this we don't have to delete `build/test-results` folder when running
    // `gradle test` instead of `gradle cleanTest test` which is hard to remember.
    val cleanTest = named("cleanTest")
    named("test") {
        dependsOn(cleanTest)
    }
}
