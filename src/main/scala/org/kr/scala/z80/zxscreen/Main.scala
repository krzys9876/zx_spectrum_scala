package org.kr.scala.z80.zxscreen

import java.awt.Color
import java.awt.image.BufferedImage
import javax.swing.Timer
import scala.annotation.unused
import scala.swing.{Frame, Graphics2D, MainFrame, Panel, SimpleSwingApplication}
import scala.swing.Swing._
import scala.swing.event.{FocusGained, FocusLost, KeyTyped}

object Main extends SimpleSwingApplication {

  private lazy val ui: Panel = new Panel {
    background = Color.white
    preferredSize = (1024, 768)
    focusable = true
    listenTo(mouse.clicks, mouse.moves, keys)

    private val image: BufferedImage = new BufferedImage(VideoMemory.XPIXELS, VideoMemory.YPIXELS, BufferedImage.TYPE_3BYTE_BGR)
    private val videoMemory: VideoMemory = VideoMemory()

    reactions += {
      //Demo actions
      case KeyTyped(_, 'r', _, _) => if(demoTimer.isRunning) demoTimer.stop() else demoTimer.start()
      case KeyTyped(_, 'f', _, _) =>
        demoTimer.stop()
        videoMemory.demoStripes()
      case KeyTyped(_, 'c', _, _) =>
        demoTimer.stop()
        videoMemory.demoColors()
      case KeyTyped(_, 'b', _, _) =>
        demoTimer.stop()
        videoMemory.demoClear()
      //Focus actions
      case _: FocusLost => repaint()
      case _: FocusGained => repaint()
    }

    private val BORDER_PERCENT: Int = 10

    private def prepareImage(): Unit = {
      val raster = image.getRaster
      val blink = (System.currentTimeMillis() & 0x003FF) < 0x0200 // millis modulo 1023 <512
      raster.setPixels(0, 0, VideoMemory.XPIXELS, VideoMemory.YPIXELS, videoMemory.getColorized(blink))
    }

    override def paintComponent(g: Graphics2D): Unit = {
      def imageStartRatio(pixels: Int): Int = pixels * BORDER_PERCENT / 100

      def imageDimensionsRatio(pixels: Int): Int = pixels * (100 - 2 * BORDER_PERCENT) / 100

      super.paintComponent(g)
      prepareImage()
      val startX = imageStartRatio(size.width)
      val startY = imageStartRatio(size.height)
      val imgWidth = imageDimensionsRatio(size.width)
      val imgHeight = imageDimensionsRatio(size.height)
      g.drawImage(image, startX, startY, imgWidth, imgHeight, null)
    }

    @unused private val refreshTimer: Timer = TimerFactory.started(50, _ => repaint())
    private val demoTimer: Timer = TimerFactory.stopped(150, _ => videoMemory.demoRandom())

    val sim=new Simulator(videoMemory)
  }

  def top: Frame = new MainFrame {
    title = "ZX"
    contents = ui
  }
}
