plugins {
    val kotlinVersion = "1.7.10"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion

    id("net.mamoe.mirai-console") version "2.15.0"
}

group = "top.mrxiaom"
version = "0.2.4"

repositories {
    maven("https://maven.aliyun.com/repository/central")
    mavenCentral()
}

dependencies {
    compileOnly("xyz.cssxsh.mirai:mirai-economy-core:1.0.0-M1")
    compileOnly("net.mamoe.yamlkt:yamlkt:0.12.0")
}

mirai {
    jvmTarget = JavaVersion.VERSION_11
}
