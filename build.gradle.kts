plugins {
    id("com.android.application") version "8.5.1" apply false
    kotlin("android") version "2.0.0" apply false
    kotlin("plugin.compose") version "2.0.0" apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
