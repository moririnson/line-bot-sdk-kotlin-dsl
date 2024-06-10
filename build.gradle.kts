plugins {
    kotlin("jvm") version "2.0.0"
    id("com.google.devtools.ksp") version "2.0.0-1.0.22" apply false
}

group = "com.linecorp"
version = "1.0-SNAPSHOT"

allprojects {
    repositories {
        mavenCentral()
    }
}

buildscript {
    dependencies {
        classpath(kotlin("gradle-plugin", version = "2.0.0"))
    }
}

sourceSets.main {
    kotlin.srcDirs("src/main/kotlin")
    java.srcDirs("src/main/kotlin")
}

tasks.test {
    useJUnitPlatform()
}