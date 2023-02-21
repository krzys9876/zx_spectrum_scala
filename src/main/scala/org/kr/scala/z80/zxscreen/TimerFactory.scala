package org.kr.scala.z80.zxscreen

import java.awt.event.ActionListener
import javax.swing.Timer

object TimerFactory {
  def started(delayMillis:Int, listener: ActionListener):Timer = create(delayMillis,started = true,listener)
  def stopped(delayMillis:Int, listener: ActionListener):Timer = create(delayMillis,started = false,listener)

  private def create(delayMillis: Int, started: Boolean, listener: ActionListener): Timer = {
    val timer = new Timer(delayMillis, listener)
    timer.setRepeats(true)
    if (started) timer.start()
    timer
  }
}
