plugins {
    id("com.android.application") version "9.2.1" apply false
    kotlin("plugin.compose") version "2.3.21" apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
