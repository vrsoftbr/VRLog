import java.io.FileInputStream
import java.util.Properties
import java.time.LocalDate
import java.time.format.DateTimeFormatter

plugins {
    id("java-library")
    id("maven-publish")
}

group = "br.com.vrsoftware"
val projectName = "VRLog"

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
//    toolchain {
//        languageVersion.set(JavaLanguageVersion.of(8))
//    }
    withJavadocJar()
    withSourcesJar()
}

repositories {
    mavenCentral()
}

val PROPERTY_FILE = "${projectDir}/src/main/resources/vrlog.properties"

fun getProperties(): Properties {
    val arquivo = File(PROPERTY_FILE)
    if (!arquivo.exists()) throw GradleException("Arquivo de versão não encontrado")

    val props = Properties().apply {
        FileInputStream(arquivo).use { load(it) }
    }
    return props
}

fun getVersao(): String {
    val props = getProperties()

    val major = props.getProperty("version.major")?.toIntOrNull() ?: 0
    val minor = props.getProperty("version.minor")?.toIntOrNull() ?: 0
    val release = props.getProperty("version.release")?.toIntOrNull() ?: 0
    val build = props.getProperty("version.build")?.toIntOrNull() ?: 0
    val beta = props.getProperty("version.beta")?.toIntOrNull() ?: 0
    val alpha = props.getProperty("version.alpha")?.toIntOrNull() ?: 0

    val version = "$major.$minor.$release-$build"

    return when {
        alpha > 0 -> "$version-alpha$alpha"
        beta > 0 -> "$version-beta$beta"
        else -> version
    }
}

version = getVersao()

tasks.register("versao") {
    description = "Exibe a versão do projeto"
    group = JavaBasePlugin.BUILD_TASK_NAME
    doFirst {
        println("$projectName v$version")
    }
}

tasks.register("release") {
    description = "Incrementa o build e atualiza a data"
    group = JavaBasePlugin.BUILD_TASK_NAME

    doLast {
        val props = getProperties()
        val currentBuild = props.getProperty("version.build")?.toIntOrNull() ?: 0
        val newBuild = currentBuild + 1
        val data = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))

        val arquivo = File(PROPERTY_FILE)
        val updatedProps = Properties().apply {
            FileInputStream(arquivo).use { load(it) }
            setProperty("version.build", newBuild.toString())
            setProperty("app.data", data)
        }

        arquivo.outputStream().use {
            updatedProps.store(it, "Updated by release task")
        }

        version = getVersao()
        println("$projectName v$version")
    }
}

tasks.jar {
    val props = getProperties()

    archiveBaseName.set(projectName)
    archiveVersion.set(version.toString())

    manifest {
        attributes(mapOf(
            "App-Date" to (props.getProperty("app.data") ?: ""),
            "Version-Major" to (props.getProperty("version.major")?.toIntOrNull() ?: 0),
            "Version-Minor" to (props.getProperty("version.minor")?.toIntOrNull() ?: 0),
            "Version-Release" to (props.getProperty("version.release")?.toIntOrNull() ?: 0),
            "Version-Build" to (props.getProperty("version.build")?.toIntOrNull() ?: 0),
            "Version-Beta" to (props.getProperty("version.beta")?.toIntOrNull() ?: 0),
            "Version-Alpha" to (props.getProperty("version.alpha")?.toIntOrNull() ?: 0),
            "Implementation-Title" to projectName,
            "Implementation-Version" to version
        ))
    }

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    doLast {
        copy {
            from("build/libs")
            into("dist")
        }
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xlint:unchecked", "-Xlint:deprecation"))
}

dependencies {

    api("ch.qos.logback:logback-classic:1.2.12")
    api("org.slf4j:slf4j-api:1.7.36")

    implementation("commons-configuration:commons-configuration:1.10")
    implementation("org.apache.commons:commons-compress:1.26.0")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}