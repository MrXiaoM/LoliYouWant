plugins {
    val kotlinVersion = "1.7.10"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion

    id("net.mamoe.mirai-console") version "2.12.3"
}

group = "top.mrxiaom"
version = "0.2.2"

repositories {
    maven("https://maven.aliyun.com/repository/central")
    mavenCentral()
}

dependencies {
    implementation("com.google.code.gson:gson:2.9.0")
    compileOnly("xyz.cssxsh.mirai:mirai-economy-core:1.0.0-M1")
}

mirai {
    jvmTarget = JavaVersion.VERSION_11
}
