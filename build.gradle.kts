import com.google.protobuf.gradle.id

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.protobuf)
    application
}

group = "com.pseudo.statcounter"
version = "0.1.0-SNAPSHOT"

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.protobuf.java)
    implementation(libs.protobuf.kotlin)

    testImplementation(kotlin("test"))
}

protobuf {
    protoc {
        artifact = libs.protobuf.protoc.get().toString()
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                id("kotlin")
            }
        }
    }
}

application {
    mainClass.set("com.pseudo.statcounter.MainKt")
}

tasks.test {
    useJUnitPlatform()
}
