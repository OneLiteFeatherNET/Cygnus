# Template Project

[[_TOC_]]

## How to prepare my project

Edit following files:
- settings.gradle.kts
- build.gradle.kts
- .gitlab-ci.yml


---
### runPaper plugin configuration

Tasks section
```kt
tasks {
    runServer {
       minecraftVersion("1.19.3")
    }
}
```


### shadowJar plugin configuration

Tasks section
```kt
tasks {
    shadowJar {
        archiveFileName.set("${rootProject.name}.${archiveExtension.getOrElse("jar")}")
    }
}
```

### bukkit plugin configuration

Bukkit section

```kt
tasks {
    TODO("")
}
bukkit {
    main = "${rootProject.group}.MAINCLASS"
    apiVersion = "1.19"
    author = "ExampleDeveloper"
    depend = listOf("FastAsyncWorldEdit")
}

```
### sonarqube plugin configuration

sonarqube section

