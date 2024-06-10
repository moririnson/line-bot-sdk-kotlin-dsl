plugins {
    application
    kotlin("jvm")
    id("com.google.devtools.ksp")
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(project(":processor"))
    implementation("com.linecorp.bot:line-bot-model:6.0.0")
    ksp(project(":processor"))
    testImplementation("org.jetbrains.kotlin:kotlin-test")
}

ksp {
    arg("ksp.debug", "true")
}

kotlin {
    jvmToolchain(17)
    sourceSets.main {
        kotlin.srcDir("build/generated/ksp/main/kotlin")
    }
    sourceSets.test {
        kotlin.srcDir("build/generated/ksp/test/kotlin")
    }
}