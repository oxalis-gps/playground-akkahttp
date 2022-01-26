import com.typesafe.sbt.packager.docker._

ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "2.13.3"

val AkkaVersion = "2.6.8"
val AkkaHttpVersion = "10.2.7"

// JavaAppPackaging

lazy val root = (project in file("."))
  .enablePlugins(FlywayPlugin, JavaServerAppPackaging, DockerPlugin)
  .settings(
    name := "playground-akkahttp",

    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
      "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
      "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion,

      "com.typesafe.akka" %% "akka-http-spray-json" % AkkaHttpVersion,

      "org.scalikejdbc" %% "scalikejdbc"       % "3.5.0",
      "org.scalikejdbc" %% "scalikejdbc-test"   % "3.5.0"   % "test",
      "org.scalikejdbc" %% "scalikejdbc-config"  % "3.5.0",

      "ch.qos.logback"  %  "logback-classic"   % "1.2.3",
      "org.mariadb.jdbc" % "mariadb-java-client" % "2.7.2",
    ),

    flywayUrl := sys.env.getOrElse("JDBC_DATABASE_URL", ""), // "jdbc:hsqldb:file:target/flyway_sample;shutdown=true",
    flywayUser := sys.env.getOrElse("JDBC_DATABASE_USERNAME", ""),
    flywayPassword := sys.env.getOrElse("JDBC_DATABASE_PASSWORD", ""),
    flywayLocations += "db/migration",
    //Test / flywayUrl := "jdbc:hsqldb:file:target/flyway_sample;shutdown=true",
    //Test / flywayUser := "SA",
    //Test / flywayPassword := ""

    run / fork := true,
    run / connectInput := true,

    Compile / run / mainClass := Some("Main"),

    dockerExposedPorts := List(8080),
    Docker / packageName := "playground-akkahttp",
    Docker / defaultLinuxInstallLocation := "/opt/application",
    executableScriptName := "app",
    dockerBaseImage := "amazonlinux:2",
    dockerUpdateLatest := true,

    dockerCommands := dockerCommands.value.take(12) ++: Seq(Cmd("RUN", "yum install -y java-11-amazon-corretto-headless shadow-utils")) ++: dockerCommands.value.drop(12)

//    dockerCommands := dockerCommands.value.filter {
//      case ExecCmd("CMD", _*) => false
//      case _ => true
//    }.map {
//      case ExecCmd("ENTRYPOINT", args @ _*) => ExecCmd("CMD", args: _*)
//      case other => other
//    },

//    dockerCommands := Seq(
//      Cmd("FROM", "amazonlinux:2 as stage0"),
//      Cmd("RUN", "yum install -y java-11-amazon-corretto-headless shadow-utils"),
//      Cmd("WORKDIR", "/opt/application"),
//      Cmd("COPY", "2/opt", "/2/opt"),
//      Cmd("COPY", "4/opt", "/4/opt"),
//      Cmd("USER", "root"),
//
//      Cmd("RUN", "chmod -R -u=rX,g=rX /2/opt/application"),
//      Cmd("RUN", "chmod -R -u=rX,g=rX /4/opt/application"),
//
//      Cmd("RUN", "chmod -R u+x,g+x /4/opt/application/bin/app"),
//
//      Cmd("FROM", "amazonlinux:2 as mainstage"),
//      Cmd("USER", "root"),
//      Cmd("RUN", "id", "-u", "demiourgos728", "1>/dev/null", "2>&1", "||", "((", "getent", "group", "0", "1>/dev/null", "2>&1", "||", "(", "type", "groupadd", "1>/dev/null", "2>&1", "&&", "groupadd", "-g", "0", "root", "||", "addgroup", "-g", "0", "-S", "root", "))", "&&", "(", "type", "useradd", "1>/dev/null", "2>&1", "&&", "useradd", "--system", "--create-home", "--uid", "1001", "--gid", "0", "demiourgos728", "||", "adduser", "-S", "-u", "1001", "-G", "root", "demiourgos728", "))"),
//      Cmd("WORKDIR", "/opt/application"),
//
//      ExecCmd("ENTRYPOINT", "/opt/docker/bin/app")
//    )
  )
