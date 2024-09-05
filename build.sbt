ThisBuild / version := "0.2.8"

ThisBuild / scalaVersion := "2.13.10"

name := "zx_screen"
organization := "org.kr.scala"

libraryDependencies ++= Seq(
  "org.scala-lang.modules" %% "scala-swing" % "3.0.0",
  "io.github.krzys9876" %% "z80_sim" % "0.2.0",
  "io.github.krzys9876" %% "command-line-reader" % "1.1.0",
  "org.scalatest" %% "scalatest" % "3.2.19" % Test
)
