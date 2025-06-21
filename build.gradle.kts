import java.io.FileInputStream
import java.util.Properties

plugins {
    id("java-library")
    id("maven-publish")
}

group = "br.com.vrsoftware"
val projectName = "VRLog"
val projectVersion = getVersao()

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8

    withJavadocJar()
    withSourcesJar()
}

repositories {
    mavenCentral()
}

fun getProperties(): Properties {

    val arquivo = File("${projectDir}/src/main/resources/vrlog.properties")
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
    val alphaVersion = "$version-alpha$alpha"
    val betaVersion = "$version-beta$beta"

    return when {
        alpha > 0 -> alphaVersion
        beta > 0 -> betaVersion
        else -> version
    }
}

tasks.register("versao") {
    description = "Exibe a versão do projeto"
    group = JavaBasePlugin.BUILD_TASK_NAME
    println("$projectName v${projectVersion}")
}

tasks.jar {

    val props = getProperties()

    archiveBaseName.set(projectName)
    archiveVersion.set(projectVersion)

    manifest {
        attributes["App-Date"] = props.getProperty("app.data") ?: ""
        attributes["Version-Major"] = props.getProperty("version.major")?.toIntOrNull() ?: 0
        attributes["Version-Minor"] = props.getProperty("version.minor")?.toIntOrNull() ?: 0
        attributes["Version-Release"] = props.getProperty("version.release")?.toIntOrNull() ?: 0
        attributes["Version-Build"] = props.getProperty("version.build")?.toIntOrNull() ?: 0
        attributes["Version-Beta"] = props.getProperty("version.beta")?.toIntOrNull() ?: 0
        attributes["Version-Alpha"] = props.getProperty("version.alpha")?.toIntOrNull() ?: 0
        attributes["Implementation-Title"] = archiveBaseName
    }

    archiveFileName.set("${projectName}_v${archiveVersion.get()}.jar")

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }) {
        exclude("META-INF/*.RSA", "META-INF/*.SF", "META-INF/*.DSA")
        exclude("META-INF/LICENSE", "META-INF/LICENSE.txt", "META-INF/NOTICE", "META-INF/NOTICE.txt")
    }

    doLast {
        copy {
            from("build/libs")
            into("dist")
            rename("($projectName).*(.jar)", "$1$2")
        }
    }
}

dependencies {

    api("ch.qos.logback:logback-classic:1.2.3")
    api("org.slf4j:slf4j-api:1.7.30")

    implementation("commons-configuration:commons-configuration:1.10")
    implementation("org.apache.commons:commons-compress:1.26.0")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}