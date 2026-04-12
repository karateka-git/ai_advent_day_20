plugins {
    kotlin("jvm") version "2.2.0"
    kotlin("plugin.serialization") version "2.2.0"
    application
}

val mcpVersion = "0.9.0"
val ktorVersion = "3.2.1"

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass = "ru.compadre.aiadvent.day16.bootstrap.BootstrapAppKt"
    applicationDefaultJvmArgs = listOf("-Dfile.encoding=UTF-8")
}

tasks.test {
    useJUnitPlatform()
}

tasks.register<JavaExec>("runServer") {
    group = "application"
    description = "Runs the MCP server entrypoint."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ru.compadre.aiadvent.day16.server.McpServerAppKt")
}

tasks.register<JavaExec>("runClient") {
    group = "application"
    description = "Runs the MCP client entrypoint."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ru.compadre.aiadvent.day16.client.McpClientAppKt")
}

dependencies {
    implementation(platform("io.ktor:ktor-bom:$ktorVersion"))
    implementation("io.modelcontextprotocol:kotlin-sdk-client:$mcpVersion")
    implementation("io.modelcontextprotocol:kotlin-sdk-server:$mcpVersion")
    implementation("io.ktor:ktor-client-cio")
    implementation("io.ktor:ktor-server-cio")
    implementation("io.ktor:ktor-server-sse")
    implementation("io.ktor:ktor-server-content-negotiation")
    implementation("io.ktor:ktor-serialization-kotlinx-json")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    runtimeOnly("org.slf4j:slf4j-simple:2.0.17")
    testImplementation(kotlin("test"))
}
