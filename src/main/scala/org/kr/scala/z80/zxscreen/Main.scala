package org.kr.scala.z80.zxscreen

import java.awt.Color
import java.awt.image.BufferedImage
import javax.swing.Timer
import scala.annotation.unused
import scala.swing.{Frame, Graphics2D, MainFrame, Panel, SimpleSwingApplication}
import scala.swing.Swing._
import scala.swing.event.{FocusGained, FocusLost, Key, KeyPressed, KeyReleased, KeyTyped}

object Main extends SimpleSwingApplication {

  private lazy val ui: Panel = new Panel {
    background = Color.white
    preferredSize = (1024, 768)
    focusable = true
    listenTo(mouse.clicks, mouse.moves, keys)

    private val image: BufferedImage = new BufferedImage(VideoMemory.XPIXELS, VideoMemory.YPIXELS, BufferedImage.TYPE_3BYTE_BGR)
    private val videoMemory: VideoMemory = VideoMemory()
    val sim=new Simulator(videoMemory)

    reactions += {
      //Demo actions
      /*case KeyTyped(_, 'r', _, _) => if(demoTimer.isRunning) demoTimer.stop() else demoTimer.start()
      case KeyTyped(_, 'f', _, _) =>
        demoTimer.stop()
        videoMemory.demoStripes()
      case KeyTyped(_, 'c', _, _) =>
        demoTimer.stop()
        videoMemory.demoColors()
      case KeyTyped(_, 'b', _, _) =>
        demoTimer.stop()
        videoMemory.demoClear()*/
      case KeyPressed(_,Key.Key0, _, _) => sim.inputPort.set(0x23)
      case KeyPressed(_,Key.Key1, _, _) => sim.inputPort.set(0x24)
      case KeyPressed(_,Key.Key2, _, _) => sim.inputPort.set(0x1C)
      case KeyPressed(_,Key.Key3, _, _) => sim.inputPort.set(0x14)
      case KeyPressed(_,Key.Key4, _, _) => sim.inputPort.set(0x0C)
      case KeyPressed(_,Key.Key5, _, _) => sim.inputPort.set(0x04)
      case KeyPressed(_,Key.Key6, _, _) => sim.inputPort.set(0x03)
      case KeyPressed(_,Key.Key7, _, _) => sim.inputPort.set(0x0B)
      case KeyPressed(_,Key.Key8, _, _) => sim.inputPort.set(0x13)
      case KeyPressed(_,Key.Key9, _, _) => sim.inputPort.set(0x1B)
      case KeyPressed(_,Key.Q, _, _) => sim.inputPort.set(0x25)
      case KeyPressed(_,Key.W, _, _) => sim.inputPort.set(0x1D)
      case KeyPressed(_,Key.E, _, _) => sim.inputPort.set(0x15)
      case KeyPressed(_,Key.R, _, _) => sim.inputPort.set(0x0D)
      case KeyPressed(_,Key.T, _, _) => sim.inputPort.set(0x05)
      case KeyPressed(_,Key.Y, _, _) => sim.inputPort.set(0x02)
      case KeyPressed(_,Key.U, _, _) => sim.inputPort.set(0x0A)
      case KeyPressed(_,Key.I, _, _) => sim.inputPort.set(0x12)
      case KeyPressed(_,Key.O, _, _) => sim.inputPort.set(0x1A)
      case KeyPressed(_,Key.P, _, _) => sim.inputPort.set(0x22)
      case KeyPressed(_,Key.A, _, _) => sim.inputPort.set(0x26)
      case KeyPressed(_,Key.S, _, _) => sim.inputPort.set(0x1E)
      case KeyPressed(_,Key.D, _, _) => sim.inputPort.set(0x16)
      case KeyPressed(_,Key.F, _, _) => sim.inputPort.set(0x0E)
      case KeyPressed(_,Key.G, _, _) => sim.inputPort.set(0x06)
      case KeyPressed(_,Key.H, _, _) => sim.inputPort.set(0x01)
      case KeyPressed(_,Key.J, _, _) => sim.inputPort.set(0x09)
      case KeyPressed(_,Key.K, _, _) => sim.inputPort.set(0x11)
      case KeyPressed(_,Key.L, _, _) => sim.inputPort.set(0x19)
      case KeyPressed(_,Key.Z, _, _) => sim.inputPort.set(0x1F)
      case KeyPressed(_,Key.X, _, _) => sim.inputPort.set(0x17)
      case KeyPressed(_,Key.C, _, _) => sim.inputPort.set(0x0F)
      case KeyPressed(_,Key.V, _, _) => sim.inputPort.set(0x07)
      case KeyPressed(_,Key.B, _, _) => sim.inputPort.set(0x00)
      case KeyPressed(_,Key.N, _, _) => sim.inputPort.set(0x08)
      case KeyPressed(_,Key.M, _, _) => sim.inputPort.set(0x18)


      case KeyPressed(_, Key.Enter, _, _) => sim.inputPort.set(0x21)
      case KeyPressed(_, Key.Space, _, _) => sim.inputPort.set(0x20)

      case KeyReleased(_, _, _, _) =>sim.inputPort.set(0x30)
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
  }

  def top: Frame = new MainFrame {
    title = "ZX"
    contents = ui
  }
}
