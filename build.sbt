ThisBuild / version := "0.1.0"

ThisBuild / scalaVersion := "2.13.10"

name := "zx_screen"
organization := "org.kr.scala"

libraryDependencies ++= Seq(
  "org.scala-lang.modules" %% "scala-swing" % "2.1.1",
  "org.scalatest" %% "scalatest" % "3.2.15" % Test
)
