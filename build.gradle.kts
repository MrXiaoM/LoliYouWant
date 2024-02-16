plugins {
    val kotlinVersion = "1.8.10"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion

    id("net.mamoe.mirai-console") version "2.16.0"
    id("com.github.gmazzo.buildconfig") version "3.1.0"
}

group = "top.mrxiaom"
version = "0.2.6"

buildConfig {
    className("BuildConstants")
    packageName("top.mrxiaom.loliyouwant")
    useKotlinOutput()

    buildConfigField("String", "VERSION", "\"${project.version}\"")
}

repositories {
    maven("https://maven.aliyun.com/repository/central")
    mavenCentral()
}

dependencies {
    compileOnly("xyz.cssxsh.mirai:mirai-economy-core:1.0.6")
    compileOnly("net.mamoe.yamlkt:yamlkt:0.12.0")

    compileOnly("org.apache.httpcomponents:httpclient:4.5.14")
}

mirai {
    jvmTarget = JavaVersion.VERSION_11
}
