plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    jvmToolchain(17)
}

application {
    // Optional PC companion node. Not required for the mobile MVP.
    mainClass.set("com.astramesh.desktop.MainKt")
}

dependencies {
    implementation(project(":core-protocol"))
    implementation(project(":core-domain"))
    implementation(project(":core-routing"))
    implementation(project(":core-security"))

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
}
