pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // 🔥 ADICIONA ESTA LINHA PARA OS GRÁFICOS FUNCIONAREM 🔥
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "GLPImobile"
include(":app")
