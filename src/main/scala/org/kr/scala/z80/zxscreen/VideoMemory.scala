package org.kr.scala.z80.zxscreen

import VideoMemory.MEMSIZE

import java.awt.Color
import scala.util.Random

case class VideoMemory() {
  val data: VideoData = new VideoData(VideoMemory.MEMSIZE, VideoMemory.PIXELS * 3)

  def setColor(value:Int):Unit = {
    val actualColorCode = value & 0x07
    data.border = VideoMemory.colors.getOrElse(actualColorCode, Color.WHITE)
  }

  def poke(addr: Int, value: Int): Unit = {
    assert(value>=0 && value<=255)
    val relative = addr - 0x4000
    relative match {
      case a if a >= 0 && a < MEMSIZE =>
        data.raw(a) = value.toByte
        drawPixelsFromMemory(a, value)
      case _ =>
    }
  }

  private def drawPixelsFromMemory(videoAddr: Int, value: Int): Unit =
    if (videoAddr < VideoMemory.MEMPIXELS) setPixels(videoAddr, value)

  private def setPixels(videoAddr: Int, value: Int): Unit = {
    val xChar = getXChar(videoAddr)
    val yPixels = getYPixel(videoAddr)
    val pixelStart = xChar * 8 + (VideoMemory.XPIXELS * yPixels)
    for (bit <- 0 until 8) {
      val bitBool = (((value & 0xFF) >> (7 - bit)) & 0x01) == 1
      data.mask(pixelStart + bit) = bitBool
    }
  }

  private def colorizeOneChar(col: Int, row: Int, blink: Boolean): Unit = {
    val (inkColor, paperColor) = getColors(col, row, blink)
    val pixelX = col * 8
    val pixelY = row * 8
    for (bitY <- 0 until 8) {
      val pixelStart = (pixelY + bitY) * VideoMemory.XPIXELS + pixelX
      for (bitX <- 0 until 8) {
        val pixel = pixelStart + bitX
        val pixelValue = data.mask(pixel)
        val color = if (pixelValue) inkColor else paperColor
        data.colorized(pixel * 3 + 0) = color.getRed
        data.colorized(pixel * 3 + 1) = color.getGreen
        data.colorized(pixel * 3 + 2) = color.getBlue
      }
    }
  }

  private def getColors(col: Int, row: Int, blink: Boolean): (Color, Color) = {
    val colorRaw = data.raw(col + row * VideoMemory.COLUMNS + VideoMemory.MEMPIXELS)
    val brightnessBitValue = (colorRaw & 0x40) >> 3
    val inkColorIndex = (colorRaw & 0x07) + brightnessBitValue
    val paperColorIndex = ((colorRaw & 0x38) >> 3) + brightnessBitValue
    val inkColor = VideoMemory.colors.getOrElse(inkColorIndex, Color.BLACK)
    val paperColor = VideoMemory.colors.getOrElse(paperColorIndex, Color.BLACK)
    val flashModeBit = ((colorRaw & 0x80) >> 7) == 1

    if (flashModeBit && blink) (paperColor, inkColor)
    else (inkColor, paperColor)
  }

  def getColorized(blink: Boolean): Array[Int] = {
    for (row <- 0 until VideoMemory.ROWS)
      for (col <- 0 until VideoMemory.COLUMNS)
        colorizeOneChar(col, row, blink)
    data.colorized
  }

  // original format of video address (16 bits): 0 1 0 Y7 | Y6 Y2 Y1 Y0 | Y5 Y4 Y3 X4 | X3 X2 X1 X0
  // leading 0 1 0 is removed before
  private def getXChar(videoAddr: Int): Int = videoAddr & 0x001F

  private def getYPixel(videoAddr: Int): Int =
    ((videoAddr & 0x0700) >> 8) +
      (((videoAddr & 0x00E0) >> 5) << 3) +
      (((videoAddr & 0x1800) >> 11) << 6)

  def demoStripes(): Unit = {
    for (addr <- 0 until VideoMemory.MEMPIXELS) poke(addr + 0x4000, 0xF0)
    for (addr <- 0 until VideoMemory.MEMCHARS) poke(addr + 0x4000 + VideoMemory.MEMPIXELS, 0x10)
  }

  def demoClear(): Unit = {
    for (addr <- 0 until VideoMemory.MEMSIZE) poke(addr + 0x4000, 0x00)
  }

  def demoRandom(): Unit =
    repeat(10000) {
      val rnd = Random.nextInt(VideoMemory.MEMSIZE) + 0x4000
      poke(rnd, Random.nextInt(0x100))
    }

  def demoColors(): Unit = {
    for (addr <- 0 until VideoMemory.MEMPIXELS) poke(addr + 0x4000, 0xFF)
    for (seg <- 0 until 3)
      for (rep <- 0 until 16)
        for (col <- 0 until 8) {
          poke(0x4000 + 0x1800 + col * 32 + seg * 32 * 8 + rep * 2, col)
          poke(0x4000 + 0x1800 + col * 32 + seg * 32 * 8 + rep * 2 + 1, col + 0x40)
        }

  }
  private def repeat(times:Int)(what: => Unit ): Unit = for(_ <- 0 until times) what
}

object VideoMemory {
  val COLUMNS: Int = 32
  val ROWS: Int = 24
  val YPIXELS: Int = ROWS * 8
  val XPIXELS: Int = COLUMNS * 8
  val MEMPIXELS: Int = COLUMNS * YPIXELS
  val MEMCHARS: Int = COLUMNS * ROWS
  val MEMSIZE: Int = MEMPIXELS + MEMCHARS
  val PIXELS: Int = XPIXELS * YPIXELS

  val colors:Map[Int,Color] = Map(
    // first 8 colors are darker (brightness attribute = 0), following 8 are brighter (1)
    0->Color.BLACK,
    1->Color.BLUE.darker(),
    2->Color.RED.darker(),
    3->Color.MAGENTA.darker(),
    4->Color.GREEN.darker(),
    5->Color.CYAN.darker(),
    6->Color.YELLOW.darker(),
    7->Color.LIGHT_GRAY,
    0+8 -> Color.BLACK,
    1+8 -> Color.BLUE,
    2+8 -> Color.RED,
    3+8 -> Color.MAGENTA,
    4+8 -> Color.GREEN,
    5+8 -> Color.CYAN,
    6+8 -> Color.YELLOW,
    7+8 -> Color.WHITE
  )
}