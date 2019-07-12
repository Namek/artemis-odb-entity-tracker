import com.moowork.gradle.node.npm.NpmTask
import com.moowork.gradle.node.task.NodeTask
import com.moowork.gradle.node.NodeExtension
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile


plugins {
    id("kotlin-multiplatform") version Deps.kotlinMultiPlatformVersion
    id("com.moowork.node") version "1.2.0"
}
repositories {
    mavenCentral()
    maven(url = "https://plugins.gradle.org/m2/")
}

apply {
    plugin("kotlin-dce-js")
    plugin("com.moowork.node")
}

configure<NodeExtension> {
    download = true
}

kotlin {
    val jvm = presets["jvmWithJava"].createTarget("jvm")

    // thanks to this `gradle check` will find Java classes within this Kotlin project.
    // We need those because the unit tests are testing the serializer serializing Java objects
    // (written in Java to be sure that Kotlin doesn't mess anything during compilation).
    val javaConvention = jvm.project.convention.getPlugin(JavaPluginConvention::class.java)
    javaConvention.sourceSets.getByName("test").java.srcDir("src/jvmTest/java")
    javaConvention.sourceSets.getByName("test").java.setOutputDir(File("build/classes/kotlin/jvm/test"))

    js {
        compilations["main"].compileKotlinTask.kotlinOptions.moduleKind = "umd"
        compilations["test"].kotlinOptions.moduleKind = "umd"
    }

    targets.add(jvm)


    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib-common"))
            }
        }
        val commonTest by getting {
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
    val compileKotlinJs by getting(Kotlin2JsCompile::class)
    val compileTestKotlinJs by getting(Kotlin2JsCompile::class)
    val jsTest by getting

    val assembleWeb = task<Copy>("assembleWeb") {
        from("src/jsMain/web/index.html")
        into("build/classes/kotlin/js/main")

        doLast {
            copy {
                from(compileKotlinJs.destinationDir)
                configurations["jsRuntimeClasspath"].forEach {
                    from(zipTree(it.absolutePath).matching { include("*.js") })
                }


                into("build/classes/kotlin/js/main/lib")
            }
        }

        dependsOn(compileKotlinJs)
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
    named("check").configure {
        dependsOn(cleanTest)
    }

    val populateNodeModulesForTests by creating {
        dependsOn(/*yarn,*/ compileKotlinJs, compileTestKotlinJs)
        doLast {
            copy {
                from(compileKotlinJs.destinationDir)
                configurations["jsRuntimeClasspath"].forEach {
                    from(zipTree(it.absolutePath).matching { include("*.js") })
                }

                into("$rootDir/node_modules")
            }

            copy {
                from(compileTestKotlinJs.destinationDir)
                configurations["jsTestRuntimeClasspath"].forEach {
                    from(zipTree(it.absolutePath).matching { include("*.js") })
                }

                into("$rootDir/node_modules")
            }

            copy {
                from("src/jsTest/test.html")
                into("$rootDir/node_modules")
            }
        }
    }

    val installMocha = task<NpmTask>("installMocha") {
        setArgs(listOf("install", "mocha"))
    }

    val runTestsWithMocha by creating(NodeTask::class) {
        dependsOn(arrayOf(populateNodeModulesForTests, installMocha))
        setScript(file("$rootDir/node_modules/mocha/bin/mocha"))
        setArgs(listOf(
            compileTestKotlinJs.outputFile,
            "--reporter-options",
            "topLevelSuite=${project.name}-tests"
        ))
    }

    jsTest.dependsOn(runTestsWithMocha)
}
