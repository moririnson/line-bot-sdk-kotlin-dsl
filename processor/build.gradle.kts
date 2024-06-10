plugins {
    kotlin("jvm")
}

dependencies {
    implementation(kotlin("reflect"))
    implementation("jakarta.annotation:jakarta.annotation-api:3.0.0")
    api("com.linecorp.bot:line-bot-model:6.0.0")
    implementation("org.projectlombok:lombok:1.18.32")
    implementation("com.squareup:kotlinpoet:1.17.0")
    implementation("com.squareup:kotlinpoet-ksp:1.17.0")
    implementation("com.google.devtools.ksp:symbol-processing-api:2.0.0-1.0.22")
}