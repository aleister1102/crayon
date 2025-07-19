plugins { id("java") }

repositories { mavenCentral() }

dependencies { compileOnly("net.portswigger.burp.extensions:montoya-api:2025.6") }

tasks.withType<JavaCompile> {
    sourceCompatibility = "21"
    targetCompatibility = "21"
    options.encoding = "UTF-8"
}

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(configurations.runtimeClasspath.get().filter { it.isDirectory })
    from(configurations.runtimeClasspath.get().filterNot { it.isDirectory }.map { zipTree(it) })
}
   archiveBaseName.set("Crayon")
    archiveVersion.set(project.version.toString())
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(configurations.runtimeClasspath.get().filter { it.isDirectory })
    from(configurations.runtimeClasspath.get().filterNot { it.isDirectory }.map { zipTree(it) })
    
    manifest {
        attributes(
            "Implementation-Title" to "Crayon",
            "Implementation-Version" to project.version,
            "Implementation-Vendor" to "aleister1102"
        )
    }
}