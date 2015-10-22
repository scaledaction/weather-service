val globalSettings = Seq(
  version := "1.0",
  scalaVersion := "2.10.5"
)

val modulePrefix = "weatherservice"

lazy val client_service = (project in file("client-service"))
  .settings(name := s"${modulePrefix}-client-service")
  .settings(globalSettings:_*)
  .settings(libraryDependencies ++= client_service_deps)

lazy val ingest_api = (project in file("ingest-api"))
  .settings(name := s"${modulePrefix}-ingest-api")
  .settings(globalSettings:_*)
  .settings(libraryDependencies ++= ingest_api_deps)

lazy val ingest_backend = (project in file("ingest-backend"))
                       .settings(name := s"${modulePrefix}-ingest-backend")
                       .settings(globalSettings:_*)
                       .settings(libraryDependencies ++= ingest_backend_deps)

val akkaVersion = "2.3.11"
val sparkVersion = "1.4.1"
val sparkCassandraConnectorVersion = "1.4.0-M3"
val kafkaVersion = "0.8.2.1"
val scalaTestVersion = "2.2.4"
val sprayVersion = "1.3.3"

lazy val client_service_deps = Seq(
  "com.typesafe.akka"      %% "akka-actor"            % akkaVersion,
  "com.typesafe.akka"      %% "akka-slf4j"            % akkaVersion,
  "io.spray"               %% "spray-can"             % sprayVersion,
  "io.spray"               %% "spray-client"          % sprayVersion,
  "io.spray"               %% "spray-routing"         % sprayVersion,
  "io.spray"               %% "spray-json"            % "1.3.2",
  "org.apache.spark"       %% "spark-sql"             % sparkVersion, //% "provided",
  "org.specs2"             %% "specs2"                % "2.2.2"        % "test",
  "io.spray"               %% "spray-testkit"         % sprayVersion   % "test",
  "com.typesafe.akka"      %% "akka-testkit"          % akkaVersion    % "test",
  "org.apache.kafka" % "kafka_2.10" % kafkaVersion
    exclude("javax.jms", "jms")
    exclude("com.sun.jdmk", "jmxtools")
    exclude("com.sun.jmx", "jmxri")
)

lazy val ingest_api_deps = Seq(
  "com.typesafe.akka"      %% "akka-actor"            % akkaVersion,
  "com.typesafe.akka"      %% "akka-slf4j"            % akkaVersion,
  "io.spray"               %% "spray-can"             % sprayVersion,
  "io.spray"               %% "spray-client"          % sprayVersion,
  "io.spray"               %% "spray-routing"         % sprayVersion,
  "io.spray"               %% "spray-json"            % "1.3.2",
  "org.specs2"             %% "specs2"                % "2.2.2"        % "test",
  "io.spray"               %% "spray-testkit"         % sprayVersion   % "test",
  "com.typesafe.akka"      %% "akka-testkit"          % akkaVersion    % "test",
  "org.apache.kafka" % "kafka_2.10" % kafkaVersion
    exclude("javax.jms", "jms")
    exclude("com.sun.jdmk", "jmxtools")
    exclude("com.sun.jmx", "jmxri")
)

lazy val ingest_backend_deps = Seq(
  "com.datastax.spark" % "spark-cassandra-connector_2.10" % sparkCassandraConnectorVersion,
  "org.apache.spark"  %% "spark-sql"             % sparkVersion, //% "provided",
  "org.apache.spark"  %% "spark-streaming"       % sparkVersion, //% "provided",
  "org.apache.spark"  %% "spark-streaming-kafka" % sparkVersion, //% "provided",
  "org.apache.spark"  %% "spark-mllib"           % sparkVersion, //% "provided",
  "io.spray"          %% "spray-client"          % sprayVersion,
  "io.spray"          %% "spray-json"            % "1.3.2",
  "com.databricks"    %% "spark-csv"             % "1.2.0"
)

