pluginManagement {
    repositories {
        jcenter()
        maven(url = "https://plugins.gradle.org/m2/")
//        maven(url = "https://dl.bintray.com/salomonbrys/gradle-plugins")
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "kotlin-multiplatform") {
                useModule("org.jetbrains.kotlin:kotlin-gradle-plugin:${requested.version}")
            }
//            if (requested.id.id.startsWith("com.github.salomonbrys.gradle.kotlin.js.")) {
//                useModule("com.github.salomonbrys.gradle.kotlin.js:kotlin-js-gradle-utils:1.2.0")
//            }
//            if (requested.id.id.startsWith("com.moowork.")) {
//                useModule("com.moowork.gradle:gradle-node-plugin:1.2.0")
//            }
        }
    }
}
rootProject.name = "artemis-odb-entity-tracker"

