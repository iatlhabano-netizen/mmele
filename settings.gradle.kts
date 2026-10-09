pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    // The Google Services plugin has no marker artifact on Google Maven; map the id explicitly.
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "com.google.gms.google-services") {
                useModule("com.google.gms:google-services:${requested.version}")
            }
        }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // Paparazzi's native layoutlib runtime: some Google Maven mirrors lack the -linux classifier.
        exclusiveContent {
            forRepository { maven("https://dl.google.com/android/maven2/") }
            filter { includeGroup("com.android.tools.layoutlib") }
        }
        google()
        mavenCentral()
    }
}
rootProject.name = "Mmele"
include(":engine", ":online", ":app")
