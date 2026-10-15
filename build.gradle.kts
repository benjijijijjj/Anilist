plugins {
    kotlin("jvm") version "2.0.21"
    application
}

group = "anilist"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    // Solo la stdlib de Kotlin (la agrega el plugin automáticamente).
    testImplementation(kotlin("test"))
}

application {
    mainClass.set("anilist.AniListKt")
}

tasks.test {
    useJUnitPlatform()
}
