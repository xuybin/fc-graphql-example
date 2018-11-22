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
        , "runJavaSh" to "io.fabric8:run-java-sh:1.2.2"
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


version = "0.1.4"
group = "com.github.xuybin"

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    compile(extra["fcGraphql"].toString())
    compile(extra["springBoot"].toString())
    compile(extra["gson"].toString())
    compile(extra["logback"].toString())
    compileOnly(extra["runJavaSh"].toString())
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
        mustRunAfter("startScripts")


        into("bin") {
            val dockerRunSh: String =
                configurations.compileOnly.get().resolve().first { it.name.startsWith("run-java-sh") }
                    .let { zipTree(it) }.first { it.name.startsWith("run-java.sh") }.absolutePath
            from("${buildDir}/scripts", dockerRunSh)
            rename {
                it.replace("run-java.sh", "run-in-docker")
            }
            eachFile {
                var scriptText = file.readText()
                    .replace("/lib/${project.name}-${project.version}.jar", "")
                    .replace("\\lib\\${project.name}-${project.version}.jar", "")
                    .replace("local cp_path=\".\"", "local cp_path=\"\${JAVA_APP_DIR}/../:\${JAVA_APP_DIR}/../lib/*\"")
                    .replace("JAVA_APP_JAR=\"\$(auto_detect_jar_file \${JAVA_APP_DIR})\"", "")
                    .replace("check_error \"\${JAVA_APP_JAR}\"", "echo \"\" >/dev/null")
                    .replace("args=\"-jar \${JAVA_APP_JAR}\"", "args=\"${application.mainClassName}\"")
                file.writeText(
                    scriptText
                )
            }
        }

        delete {
            fileTree("${buildDir}/resources/main") {
                include("**/*-dev.properties")
            }
        }
    }
    startScripts {
        applicationName = "run"
    }
    distTar { enabled = false }
    distZip { enabled = false }

    processResources {
        filesMatching("**/*.properties") {
            expand(project.properties)
        }
        filesMatching("**/*.xml") {
            expand(project.properties)
        }
    }

    compileKotlin {
        kotlinOptions {
            freeCompilerArgs = listOf("-Xjsr305=strict")
            jvmTarget = "1.8"
        }
        serviceLoaderGen("com.github.xuybin.fc.graphql.GApp")
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
        "${project.projectDir}${File.separator}src${File.separator}main${File.separator}resources${File.separator}META-INF${File.separator}services${File.separator}$serviceName"
   println("servicePath-$servicePath")
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
