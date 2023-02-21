package org.kr.scala.z80.zxscreen

import java.awt.Color
import java.awt.event.ActionEvent
import java.awt.image.BufferedImage
import javax.swing.Timer
import scala.swing.{Frame, Graphics2D, MainFrame, Panel, SimpleSwingApplication}
import scala.swing.Swing._
import scala.swing.event.{FocusGained, FocusLost, KeyTyped}

object Main extends SimpleSwingApplication {

  private lazy val ui: Panel = new Panel {
    background = Color.white
    preferredSize = (500, 400)
    focusable = true
    listenTo(mouse.clicks, mouse.moves, keys)

    val image:BufferedImage = new BufferedImage(VideoMemory.XPIXELS,VideoMemory.YPIXELS,BufferedImage.TYPE_3BYTE_BGR)

    val videoMemory:VideoMemory=VideoMemory()

    def doPaint(): Unit = {
      val raster = image.getRaster
      val blink = (System.currentTimeMillis() & 0x003FF) < 0x0200 // millis modulo 1024 <512
      raster.setPixels(0, 0, VideoMemory.XPIXELS, VideoMemory.YPIXELS, videoMemory.getColorized(blink))
    }

    reactions += {
      case KeyTyped(_, 'r', _, _) => videoMemory.demoRandom()
      case KeyTyped(_, 'f', _, _) => videoMemory.demoStripes()
      case KeyTyped(_, 'c', _, _) => videoMemory.demoColors()

      case _: FocusLost => repaint()
      case _: FocusGained => repaint()
    }

    override def paintComponent(g: Graphics2D): Unit = {
      super.paintComponent(g)
      doPaint()
      val startX=size.width * 1 / 10
      val startY=size.height * 1 / 10
      val imgWidth=size.width * 8 / 10
      val imgHeight=size.height * 8 / 10
      g.drawImage(image,startX,startY,imgWidth,imgHeight,null)
    }

    val timer:Timer = new Timer(0, _ => repaint())
    timer.setRepeats(true)
    timer.setDelay(50)
    timer.start()
  }

  def top: Frame = new MainFrame {
    title = "ZX"
    contents = ui
  }
}
