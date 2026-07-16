plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    jvmToolchain(17)
    sourceSets.all {
        kotlin.srcDir("src/${name}/kotlin")
    }
}

dependencies {
    api(project(":core-protocol"))
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
}
