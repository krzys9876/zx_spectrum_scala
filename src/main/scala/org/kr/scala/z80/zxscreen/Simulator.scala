package org.kr.scala.z80.zxscreen

import org.kr.scala.z80.system.{CyclicInterrupt, Debugger, DummyDebugger, InputFile, InputPort, InputPortConsole, InputPortControlConsole, MemoryContents, MemoryHandler, MutableMemory, OutputFile, PortID, Register, StateWatcher, Z80System}
import org.kr.scala.z80.utils.Z80Utils

import java.nio.file.{Files, Path}
import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.swing.event.Key
import scala.swing.event.Key.{Modifier, Modifiers}

class Simulator(val video:VideoMemory) {
  val CONTROL_PORT = PortID(0xB1)
  val DATA_PORT = PortID(0xB0)

  val hexFile="input-files\\zx82_rom_KR_orig.hex"
  //val hexFile="input-files\\zx82_rom_KR_mod01.hex"
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
  private val keyb:mutable.Set[ZXKeyCoords] = mutable.Set()

  override def read(upperAddr:Int):Int= {
    (0 to 7).foldLeft(InputPortZXKey.NONE)((output, bit)=>{
      val bitToCheck=(upperAddr & (1 << bit)) == 0
      val maskForPressedKeys = if(bitToCheck) getMaskforBit(bit) else InputPortZXKey.NONE
      output & maskForPressedKeys
    })
  }
  override def refresh():InputPort=this
  def addKey(key: Key.Value): Unit = {
    val coords=ZXKeyCoords.keysMap.get(key)
    coords.map(c => keyb+=c)
  }
  def addModifiers(modifiers: Modifiers): Unit = {
    if((modifiers & Modifier.Shift)>0) addKey(Key.Shift)
    if((modifiers & Modifier.Control)>0) addKey(Key.Control)
  }
  def removeKey(key: Key.Value): Unit = {
    val coords = ZXKeyCoords.keysMap.get(key)
    coords.map(c => keyb-=c)
  }
  def removeModifiers(modifiers: Modifiers): Unit = {
    if ((modifiers & Modifier.Shift) > 0) removeKey(Key.Shift)
    if ((modifiers & Modifier.Control) > 0) removeKey(Key.Control)
  }

  private def getMaskforBit(bit:Int):Int = {
    val keysForBit = keyb.filter(_.inputBit == bit)
    keysForBit.foldLeft(InputPortZXKey.NONE)((keysOutput, keyCoords) =>
      keysOutput & ((~(1 << keyCoords.outputBit)) & InputPortZXKey.NONE))
  }
}

object InputPortZXKey {
  val NONE:Int=0x1F
}

case class ZXKeyCoords(inputBit:Int,outputBit:Int)

object ZXKeyCoords {
  //rows:
  val keysMap:Map[Key.Value,ZXKeyCoords] = Map(
    Key.Key1->ZXKeyCoords(3,0),
    Key.Key2->ZXKeyCoords(3,1),
    Key.Key3->ZXKeyCoords(3,2),
    Key.Key4->ZXKeyCoords(3,3),
    Key.Key5->ZXKeyCoords(3,4),
    Key.Key6->ZXKeyCoords(4,4),
    Key.Key7->ZXKeyCoords(4,3),
    Key.Key8->ZXKeyCoords(4,2),
    Key.Key9->ZXKeyCoords(4,1),
    Key.Key0->ZXKeyCoords(4,0),
    Key.Q->ZXKeyCoords(2,0),
    Key.W->ZXKeyCoords(2,1),
    Key.E->ZXKeyCoords(2,2),
    Key.R->ZXKeyCoords(2,3),
    Key.T->ZXKeyCoords(2,4),
    Key.Y->ZXKeyCoords(5,4),
    Key.U->ZXKeyCoords(5,3),
    Key.I->ZXKeyCoords(5,2),
    Key.O->ZXKeyCoords(5,1),
    Key.P->ZXKeyCoords(5,0),
    Key.A -> ZXKeyCoords(1, 0),
    Key.S -> ZXKeyCoords(1, 1),
    Key.D -> ZXKeyCoords(1, 2),
    Key.F -> ZXKeyCoords(1, 3),
    Key.G -> ZXKeyCoords(1, 4),
    Key.H -> ZXKeyCoords(6, 4),
    Key.J -> ZXKeyCoords(6, 3),
    Key.K -> ZXKeyCoords(6, 2),
    Key.L -> ZXKeyCoords(6, 1),
    Key.Enter -> ZXKeyCoords(6, 0),
    Key.Shift -> ZXKeyCoords(0, 0),
    Key.Z -> ZXKeyCoords(0, 1),
    Key.X -> ZXKeyCoords(0, 2),
    Key.C -> ZXKeyCoords(0, 3),
    Key.V -> ZXKeyCoords(0, 4),
    Key.B -> ZXKeyCoords(7, 4),
    Key.N -> ZXKeyCoords(7, 3),
    Key.M -> ZXKeyCoords(7, 2),
    Key.Control -> ZXKeyCoords(7, 1),
    Key.Space -> ZXKeyCoords(7, 0),
  )
}