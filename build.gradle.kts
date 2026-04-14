import org.gradle.api.tasks.Sync
import org.gradle.jvm.application.tasks.CreateStartScripts
import org.gradle.jvm.tasks.Jar

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

val launcherJvmArgs = listOf(
    "-Dfile.encoding=UTF-8",
    "-Dstdout.encoding=UTF-8",
    "-Dstderr.encoding=UTF-8",
)
val packagedRuntimeClasspath = files(
    tasks.named<Jar>("jar").flatMap { it.archiveFile },
    configurations.runtimeClasspath,
)

application {
    mainClass = "ru.compadre.mcp.AppKt"
    applicationDefaultJvmArgs = launcherJvmArgs
}

tasks.named<CreateStartScripts>("startScripts") {
    applicationName = "mcp-client"
    defaultJvmOpts = launcherJvmArgs
}

val serverStartScripts = tasks.register<CreateStartScripts>("serverStartScripts") {
    applicationName = "mcp-server"
    mainClass = "ru.compadre.mcp.mcp.server.stateless.StatelessMcpServerAppKt"
    classpath = packagedRuntimeClasspath
    outputDir = layout.buildDirectory.dir("generated/server-start-scripts").get().asFile
    defaultJvmOpts = launcherJvmArgs
}

val statefulServerStartScripts = tasks.register<CreateStartScripts>("statefulServerStartScripts") {
    applicationName = "mcp-stateful-server"
    mainClass = "ru.compadre.mcp.mcp.server.stateful.StatefulMcpServerAppKt"
    classpath = packagedRuntimeClasspath
    outputDir = layout.buildDirectory.dir("generated/stateful-server-start-scripts").get().asFile
    defaultJvmOpts = launcherJvmArgs
}

tasks.test {
    useJUnitPlatform()
}

tasks.register<JavaExec>("runServer") {
    group = "application"
    description = "Runs the MCP server entrypoint."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ru.compadre.mcp.mcp.server.stateless.StatelessMcpServerAppKt")
}

tasks.register<JavaExec>("runStatefulServer") {
    group = "application"
    description = "Runs the stateful MCP server entrypoint."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ru.compadre.mcp.mcp.server.stateful.StatefulMcpServerAppKt")
}

tasks.register<JavaExec>("runClient") {
    group = "application"
    description = "Runs the MCP client entrypoint."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ru.compadre.mcp.AppKt")
    standardInput = System.`in`
}

tasks.register<Sync>("installClientDist") {
    group = "distribution"
    description = "Builds a direct client launcher distribution."
    dependsOn(tasks.named("jar"), tasks.named("startScripts"))
    into(layout.buildDirectory.dir("install/mcp-client"))

    from(tasks.named<CreateStartScripts>("startScripts")) {
        into("bin")
    }
    from(tasks.named<Jar>("jar")) {
        into("lib")
    }
    from(configurations.runtimeClasspath) {
        into("lib")
    }
}

tasks.register<Sync>("installServerDist") {
    group = "distribution"
    description = "Builds a direct server launcher distribution."
    dependsOn(tasks.named("jar"), serverStartScripts)
    into(layout.buildDirectory.dir("install/mcp-server"))

    from(serverStartScripts) {
        into("bin")
    }
    from(tasks.named<Jar>("jar")) {
        into("lib")
    }
    from(configurations.runtimeClasspath) {
        into("lib")
    }
}

tasks.register<Sync>("installStatefulServerDist") {
    group = "distribution"
    description = "Builds a direct stateful server launcher distribution."
    dependsOn(tasks.named("jar"), statefulServerStartScripts)
    into(layout.buildDirectory.dir("install/mcp-stateful-server"))

    from(statefulServerStartScripts) {
        into("bin")
    }
    from(tasks.named<Jar>("jar")) {
        into("lib")
    }
    from(configurations.runtimeClasspath) {
        into("lib")
    }
}

dependencies {
    implementation(platform("io.ktor:ktor-bom:$ktorVersion"))
    implementation("io.modelcontextprotocol:kotlin-sdk-client:$mcpVersion")
    implementation("io.modelcontextprotocol:kotlin-sdk-server:$mcpVersion")
    implementation("io.ktor:ktor-client-cio")
    implementation("io.ktor:ktor-client-content-negotiation")
    implementation("io.ktor:ktor-sse:$ktorVersion")
    implementation("io.ktor:ktor-server-cio")
    implementation("io.ktor:ktor-server-sse")
    implementation("io.ktor:ktor-server-content-negotiation")
    implementation("io.ktor:ktor-serialization-kotlinx-json")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    runtimeOnly("org.slf4j:slf4j-simple:2.0.17")
    testImplementation(kotlin("test"))
}
