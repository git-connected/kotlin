plugins {
    kotlin("jvm")
    id("jps-compatible")
}

repositories {
    maven("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies")
}

val jflexPath by configurations.creating

dependencies {
    val compile by configurations
    val compileOnly by configurations

    compile(project(":core:compiler.common"))
    compile(project(":compiler:util"))
    compile(project(":compiler:frontend.common"))
    compile(project(":kotlin-script-runtime"))

    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }
    compileOnly(intellijDep()) { includeJars("guava", "trove4j", rootProject = rootProject) }

    jflexPath(commonDep("org.jetbrains.intellij.deps.jflex", "jflex"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}



ant.importBuild("buildLexer.xml")

ant.properties["builddir"] = buildDir.absolutePath

tasks.findByName("lexer")!!.apply {
    doFirst {
        ant.properties["flex.classpath"] = jflexPath.asPath
    }
}
