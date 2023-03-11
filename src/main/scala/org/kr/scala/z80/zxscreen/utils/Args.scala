package org.kr.scala.z80.zxscreen.utils

import org.kr.args.{ArgsAsClass, Argument}

class Args(args: Array[String]) extends ArgsAsClass(args) {
  val waitMs:Argument[Int] = Argument.optional(20)
  val tapFile:Argument[String] = Argument.optional("")
  val saveTapPrefix:Argument[String] = Argument.optional("tap_")
  val saveTapTimestamp:Argument[Boolean] = Argument.optional(true)
  parse()
}
