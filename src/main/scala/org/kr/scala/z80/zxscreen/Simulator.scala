package org.kr.scala.z80.zxscreen

import org.kr.scala.z80.system.{CyclicInterrupt, Debugger, DummyDebugger, InputFile, InputPort, InputPortConsole, InputPortControlConsole, MemoryContents, MemoryHandler, MutableMemory, OutputFile, PortID, Register, StateWatcher, Z80System}
import org.kr.scala.z80.utils.Z80Utils

import java.nio.file.{Files, Path}
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters.CollectionHasAsScala

class Simulator(val video:VideoMemory) {
  val CONTROL_PORT = PortID(0xB1)
  val DATA_PORT = PortID(0xB0)

  val hexFile="input-files\\zx82_rom_KR_mod01.hex"
  //val hexFile="input-files\\basicall_KR_simpleIO.hex"

  //implicit val debugger: Debugger = ConsoleDebugger
  implicit val debugger: Debugger = DummyDebugger
  //implicit val debugger: Debugger = ConsoleDetailedDebugger
  implicit val memoryHandler: MemoryHandler = new MutableZXMemoryHandler(video)
  val memory=prepareMemory
  val inputPort=new InputPortZXKey
  val initSystem=new Z80System(memory,Register.blank,OutputFile.blank,prepareInput2(inputPort),0,CyclicInterrupt.every20ms)

  import ExecutionContext.Implicits._
  val after=Future(StateWatcher[Z80System](initSystem) >>== Z80System.run(debugger)(Long.MaxValue))


  private def prepareMemory(implicit memoryHandler: MemoryHandler): MemoryContents =
    (StateWatcher(memoryHandler.blank(0x10000)) >>==
      memoryHandler.loadHexLines(readFile(hexFile)) >>==
      memoryHandler.lockTo(0x4000))
      //memoryHandler.lockTo(0x2000))
      .state

  private def readTextFile(inputTextFile: String): List[String] =
    if (inputTextFile.nonEmpty) readFile(inputTextFile)
    else List()

  private def readFile(fullFileWithPath: String): List[String] =
    Files.readAllLines(Path.of(fullFileWithPath)).asScala.toList

  private def prepareInput: InputFile = {
    val inputLines = readTextFile("input-files\\fillmem.txt")
    val chars = if (inputLines.nonEmpty) inputLines.foldLeft("")((fullString, line) => fullString + line + "\r") else ""
    val consolePort = new InputPortConsole(chars.toCharArray)
    val controlPort=new InputPortControlConsole(consolePort)
    InputFile.blank
      .attachPort(CONTROL_PORT, controlPort)
      .attachPort(DATA_PORT, consolePort)
  }

  private def prepareInput2(port:InputPort): InputFile = {
    InputFile.blank
      //.attachPort(PortID(0xFE), new InputPortConstant(0x30))
      .attachPort(PortID(0xFE),port)
  }

}

class MutableZXMemoryHandler(val video:VideoMemory) extends MemoryHandler {
  override def blank(size:Int):MemoryContents=new MutableMemory(Vector.fill(size)(0),size)
  def preloaded(initial:Vector[Int],size:Int):MemoryContents= new MutableMemory(initial++Vector.fill(size-initial.size)(0),size)
  // functions changing state (Memory=>Memory)
  override def poke: (Int, Int) => MemoryContents => MemoryContents = (address, value) => memory => {
    videoPoke(address,value)
    memory.poke(address, value)
  }
  override def pokeW: (Int, Int) => MemoryContents => MemoryContents = (address, value) => memory => {
    videoPoke(address, Z80Utils.getL(value))
    videoPoke(address+1, Z80Utils.getH(value))
    memory.pokeW(address, value)
  }
  override def pokeMulti: (Int, Vector[Int]) => MemoryContents => MemoryContents = (address, values) => memory => {
    values.foldLeft(address)((i,value)=>{
      videoPoke(i,value)
      i+1
    })
    memory.pokeMulti(address, values)
  }
  override def loadHexLines: List[String] => MemoryContents => MemoryContents = lines => memory => memory.loadHexLines(lines)
  override def lockTo: Int => MemoryContents => MemoryContents = upperAddressExcl => memory => memory.lockTo(upperAddressExcl)

  private def videoPoke(address:Int,value:Int):Unit = video.poke(address,value)
}

class InputPortZXKey extends InputPort {
  private var key:Int = InputPortZXKey.NONE

  override def read(upperAddr:Int):Int= key
  override def refresh():InputPort=this
  def set(value: Int): Unit = key=value
}

object InputPortZXKey {
  val NONE:Int=0x30
}