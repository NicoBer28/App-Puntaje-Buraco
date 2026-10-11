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
    }
}

rootProject.name = "Puntaje Buraco 2.0"
// Un módulo por capa: el compilador impide que una capa use otra que no debe.
// :app (ui + di) -> :data -> :domain, y :app -> :domain.
include(":app", ":data", ":domain")
