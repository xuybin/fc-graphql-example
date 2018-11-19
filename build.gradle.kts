import com.moowork.gradle.node.npm.NpmTask
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes

buildscript {
    mapOf(
        "springProfile" to "",
        "fcGraphql" to "com.github.xuybin:fc-graphql:1.0.0"
        , "springBoot" to "org.springframework.boot:spring-boot:2.0.6.RELEASE"
        , "gson" to "com.google.code.gson:gson:2.8.5"
        , "logback" to "ch.qos.logback:logback-classic:1.2.3"
    ).entries.forEach {
        extra.set(it.key, parent?.extra?.run { if (has(it.key)) get(it.key) else null } ?: it.value)
    }
}

plugins {
    application
    kotlin("jvm") version "1.3.10"
    kotlin("plugin.spring") version "1.3.10"
    id("com.moowork.node") version "1.2.0"
}


version = "0.1.0"
group = "com.github.xuybin"

repositories {
    mavenCentral()
    jcenter()
    maven("https://dl.bintray.com/xuybin/maven")
}

dependencies {
    compile(extra["fcGraphql"].toString())
    compile(extra["springBoot"].toString())
    compile(extra["gson"].toString())
    compile(extra["logback"].toString())
}

node {
    version = "8.12.0"
    distBaseUrl = "https://npm.taobao.org/mirrors/node"
    download = true
}

tasks {
    application.mainClassName = "com.github.xuybin.fc.graphql.MainKt"
    named<JavaExec>("run") {
        classpath = sourceSets["main"].runtimeClasspath
        args = listOf("--spring.profiles.active=dev")
        //jvmArgs = listOf("-Dspring.profiles.active=dev")
    }
    startScripts{
        //classpath = files("path/to/some.jar")
        //outputDir = file("build/sample")
    }
    distZip{
        eachFile{
            path=path.replace("${project.name}-${project.version}/","")
        }
    }
    distTar{
        enabled=false
    }

    register("funDeploy", NpmTask::class) {
        group = "Node"
        project.copy {
            from("${project.projectDir}")
            include("*.tpl.yml")
            rename {
                it.replace(".tpl.yml", ".yml")
            }
            expand(project.properties)
            into("${project.projectDir}")
        }
        doFirst {
            setNpmCommand("install", "@alicloud/fun", "-g")
        }
        doLast {
            Files.walkFileTree(Paths.get("${node.workDir}"), object : SimpleFileVisitor<java.nio.file.Path>() {
                override fun visitFile(file: java.nio.file.Path, attrs: BasicFileAttributes): FileVisitResult {
                    if ("$file".endsWith(
                            org.gradle.internal.os.OperatingSystem.current().getScriptName("fun").replace(
                                ".bat",
                                ".cmd"
                            )
                        )
                    ) {
                        exec {
                            commandLine("$file", "-v")
                        }
                        exec {
                            commandLine("$file", "deploy")
                        }
                        return FileVisitResult.TERMINATE
                    } else {
                        return FileVisitResult.CONTINUE
                    }
                }
            })
        }
    }
    jar {
        manifest {
            attributes(
                mapOf(
                    "Main-Class" to application.mainClassName
                    , "Implementation-Title" to project.name
                    , "Implementation-Version" to project.version
                )
            )
        }
        into("lib") {
            from(configurations.compile.get().resolve().map { if (it.isDirectory) it else it })
        }
        delete{
            fileTree("${buildDir}/resources/main") {
                include("**/*-dev.properties")
            }
        }
    }

    processResources {
        filesMatching("**/*.properties") {
            expand(project.properties)
        }
        filesMatching("**/*.xml") {
            expand(project.properties)
        }
        serviceLoaderGen("com.github.xuybin.fc.graphql.GApp")
    }

    compileKotlin {
        kotlinOptions {
            freeCompilerArgs = listOf("-Xjsr305=strict")
            jvmTarget = "1.8"
        }
    }

    compileTestKotlin {
        kotlinOptions {
            freeCompilerArgs = listOf("-Xjsr305=strict")
            jvmTarget = "1.8"
        }
    }
}

fun serviceLoaderGen(serviceName: String) {
    val servicePath =
        "${project.buildDir}${File.separator}resources${File.separator}main${File.separator}META-INF${File.separator}services${File.separator}$serviceName"
    var serviceImpls = mutableSetOf<String>()
    project.sourceSets["main"].allSource.forEach {
        if (it.name.endsWith(".kt")) {
            val srcTxt = it.readText()
            var packageName: String = ""
            "package\\s+([a-zA-Z_0-9.]+)\\s+".toRegex().findAll(srcTxt).forEach {
                packageName = it.groupValues[1]
                return@forEach
            }
            "class\\s+([a-zA-Z_0-9\\(\\)]+)\\s*:\\s*[a-zA-Z_0-9\\(\\)|\\s|,]*${serviceName.substringAfterLast(".")}".toRegex()
                .findAll(srcTxt)
                .forEach {
                    serviceImpls.add("$packageName.${it.groupValues[1]}")
                }
        }
    }
    if (serviceImpls.size > 0) {
        File(servicePath).also {
            it.parentFile.mkdirs()
            it.writeText(serviceImpls.joinToString("\n"))
        }
    }
}
