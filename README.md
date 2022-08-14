# Template Project

[[_TOC_]]

## How to prepare my project

Edit following files:
- settings.gradle.kts
- build.gradle.kts
- .gitlab-ci.yml
- gradle/libs.versions.toml


---

## Important information

MariaDB Login Credentials

| Username | Password | 
|----------|----------|
| root     | password |

[Docker Repo](https://hub.docker.com/_/mariadb)

---

Redis Login Credentials
- No login data required!


**We are using a fork of redis that is multithreaded**

[Docker Repo](https://registry.hub.docker.com/r/eqalpha/keydb)

---

### Dependencies

| Name                 | Version              | Group                  | ArtifactID             | 
|----------------------|----------------------|------------------------|------------------------|
| paper                | 1.18.2-R0.1-SNAPSHOT | io.papermc.paper       | paper                  |
| cloudnetPerms        | 3.4.5-RLEASE         | de.dytanic.cloudnet    | cloudnet-perms         |
| cloudnetBrdige       | 3.4.5-RLEASE         | de.dytanic.cloudnet    | clodunet-bridge        |
| cloudnetDriver       | 3.4.5-RLEASE         | de.dytanic.cloudnet    | cloudnet-driver        |
| cloudPaper           | 1.6.2                | cloud.commandframework | cloud-paper            |
| cloudAnnotations     | 1.6.2                | cloud.commandframework | cloud-annotations      |
| cloudMinecraftExtras | 1.6.2                | cloud.commandframework | cloud-minecraft-extras |
| commdore             | 1.13                 | me.lucko               | commodore              |
| hibernate            | 6.1.0.Final          | org.hibernate          | hibernate-core         |
| hibernateEnvers      | 6.1.0.Final          | org.hibernate          | hibernate-envers       |
| hibernateHikariCP    | 6.1.0.Final          | org.hibernate          | hibernate-hikaricp     |
| mariadbJavaClient    | 3.0.5                | org.mariadb.jdbc       | mariadb-java-client    |
| redisson             | 3.17.3               | org.redisson           | redisson               |
| liquibaseCore        | 3.4.1                | org.liquibase          | liquibase-core         |
| liquibaseHibernate5  | 4.9.1                | org.liquibase.ext      | liquibase-hibernate5   |


### Plugins

| Id                              | Version | 
|---------------------------------|---------|
| com.github.johnrengelman.shadow | 7.1.2   |
| xyz.jpenilla.run-paper          | 1.0.6   |
| net.minecrell.plugin-yml.bukkit | 0.5.1   |
| org.liquibase.gradle            | 2.1.0   |


### runPaper plugin configuration

Tasks section
```kt
tasks {
    runServer {
       minecraftVersion("1.18.2")
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
    apiVersion = "1.18"

    authors = listOf("TheMeinerLP", "OneLiteFeather")

    depend = listOf("helper") // Hard depended
    softDepend = listOf("CloudNet-Bridge") // Soft dependencies 
}

```


### liquibase plugin configuration

liquibase section
```kt
tasks {
    TODO("")
}
liquibase {
    activities {
        create("diffMain") {
            (this.arguments as MutableMap<String, String>).apply {
                this["changeLogFile"] = "src/main/resources/db/changelog/db.changelog-diff.xml"
                this["url"] = "jdbc:mariadb://localhost:3306/elytrarace"
                this["username"] = "root"
                this["password"] = "%Schueler90"
// set e.g. the Dev Database to perform diffs
                this["referenceUrl"] = "jdbc:mariadb://localhost:3306/elytraracediff"
                this["referenceUsername"] = "root"
                this["referencePassword"] = "%Schueler90"
            }
        }
    }
}
```

### sonarqube plugin configuration

sonarqube section

