package org.kr.scala.z80.zxscreen.test

import org.kr.scala.z80.zxscreen.VideoMemory
import org.scalatest.GivenWhenThen
import org.scalatest.featurespec.AnyFeatureSpec

import java.awt.Color

class TestVideoMemory extends AnyFeatureSpec with GivenWhenThen {
  Feature("translate pixel address to image") {
    Scenario("valid pixel address to image (line 0)") {
      Given("a valid address and a value")
      val addr=0x4000 + 1 // col 1 row 0
      val value=0x55
      val videoMemory = VideoMemory()
      When("poked to video memory")
      videoMemory.poke(addr, value)
      Then("image data is properly populated")
      assert(bitsToByte(1 * 8,videoMemory.data.mask) == 0x55)
    }
    Scenario("valid pixel address to image (1st Y segment)") {
      Given("a valid address and a value")
      val addr = 0x4000 + 2 + 0x100 * 3 // col 2 row 3
      val value = 0x25
      val videoMemory = VideoMemory()
      When("poked to video memory")
      videoMemory.poke(addr, value)
      Then("image data is properly populated")
      assert(bitsToByte(3*32*8 + 2*8, videoMemory.data.mask) == 0x25)
    }
    Scenario("valid pixel address to image (2nd Y segment)") {
      Given("a valid address and a value")
      val addr = 0x4000 + 5 + 0x100 * 3 + 0x020 * 2 // col 5 row 19 (3 + 2x8)
      val value = 0x82
      val videoMemory = VideoMemory()
      When("poked to video memory")
      videoMemory.poke(addr, value)
      Then("image data is properly populated")
      assert(bitsToByte(19 * 32 * 8 + 5 * 8, videoMemory.data.mask) == 0x82)
    }
    Scenario("valid pixel address to image (3rd Y segment)") {
      Given("a valid address and a value")
      val addr = 0x4000 + 15 + 0x100 * 3 + 0x020 * 2 + 0x0800 * 1 // col 15 row 83 (3 + 2x8 + 1x64)
      val value = 0x54
      val videoMemory = VideoMemory()
      When("poked to video memory")
      videoMemory.poke(addr, value)
      Then("image data is properly populated")
      assert(bitsToByte(83 * 32 * 8 + 15 * 8, videoMemory.data.mask) == 0x54)
    }
  }
  Feature("colorize pixels (8x8) with ink and paper colors") {
    Scenario("verify colorized area") {
      Given("a color set in selected area and bit mask")
      val cell=35 // column 3, row 1 (counting 8x8 characters from 0)
      val colorValue = 0x01 + (0x02 << 3) // ink: red, paper: blue, bright: off, flash: off
      val pixelAddr1 = 0x4000 + 3 + 0x100 * 0 + 0x020 * 1 // col 3 (char) row 8 (pixels)
      val pixelAddr2 = 0x4000 + 3 + 0x100 * 7 + 0x020 * 1 // col 3 (char) row 8+7 (pixels)
      val pixelValue = 0xF0
      val videoMemory = VideoMemory()
      videoMemory.poke(0x4000 + 0x1800 + cell,colorValue)
      videoMemory.poke(pixelAddr1,pixelValue)
      videoMemory.poke(pixelAddr2,pixelValue)
      When("pixels are colorized (i.e. image is generated)")
      val colorized=videoMemory.getColorized(false)
      Then("pixels of value 1 are set to ink color")
      And("pixels of value 0 are set to paper color")
      // first for of the character cell
      assert(bitsToByte(8*256+3*8,videoMemory.data.mask)==0xF0)
      assert(pixelsToColor((8 * 256 + 3 * 8) * 3, colorized) == VideoMemory.colors(0x01))
      assert(pixelsToColor((8 * 256 + 3 * 8 + 4) * 3, colorized) == VideoMemory.colors(0x02))
      // last row for of the character cell
      assert(bitsToByte((8+7)*256+3*8,videoMemory.data.mask)==0xF0)
      assert(pixelsToColor(((8+7) * 256 + 3 * 8) * 3, colorized) == VideoMemory.colors(0x01))
      assert(pixelsToColor(((8+7) * 256 + 3 * 8 + 4) * 3, colorized) == VideoMemory.colors(0x02))
    }

  }

  private def bitsToByte(start:Int,array:Array[Boolean]):Int =
    List.range(0,8).foldLeft(0)((agg,bit)=>agg+(bitToNum(array(start+bit)) << (7-bit)))

  private def bitToNum(bit:Boolean):Int = if(bit) 1 else 0

  private def pixelsToColor(start:Int,data:Array[Int]):Color = new Color(data(start),data(start+1),data(start+2))

}
