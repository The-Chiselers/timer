// See README.md for license details.

ThisBuild / scalaVersion     := "2.13.15"
ThisBuild / version          := "0.1.0"
ThisBuild / organization     := "tech.rocksavage"
ThisBuild / organizationName := "Rocksavage Technology"

//Test / parallelExecution := false

val chiselVersion   = "6.6.0"
val scalafmtVersion = "2.5.0"

lazy val synth = RootProject(uri("https://github.com/The-Chiselers/synth.git#main"))
lazy val addrdecode = RootProject(uri("https://github.com/The-Chiselers/addrdecode.git#main"))
lazy val apbinterface = RootProject(uri("https://github.com/The-Chiselers/apbinterface.git#main"))
lazy val addressablemodule = RootProject(uri("https://github.com/The-Chiselers/addressablemodule.git#main"))
lazy val root = (project in file("."))
  .settings(
    name                   := "timer",
    Test / publishArtifact := true,
    libraryDependencies ++= Seq(
      "org.chipsalliance" %% "chisel"     % chiselVersion,
      "edu.berkeley.cs"   %% "chiseltest" % "6.0.0",
      "org.rogach"        %% "scallop"    % "5.2.0"
    ),
    scalacOptions ++= Seq(
      "-language:reflectiveCalls",
      "-deprecation",
      "-feature",
      "-Xcheckinit",
      "-Ymacro-annotations"
    ),
    addCompilerPlugin(
      "org.chipsalliance" % "chisel-plugin" % chiselVersion cross CrossVersion.full
    )
  )
  .dependsOn(addressablemodule)
  .dependsOn(synth)
  .dependsOn(addrdecode)
  .dependsOn(apbinterface)


// Scala coverage settings
coverageDataDir            := target.value / "../generated/scalaCoverage"
coverageFailOnMinimum      := true
coverageMinimumStmtTotal   := 90
coverageMinimumBranchTotal := 95
