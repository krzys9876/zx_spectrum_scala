package org.kr.scala.z80.zxscreen

import org.kr.scala.z80.system.{ConsoleDebugger, ConsoleDetailedDebugger, CyclicInterrupt, Debugger, DummyDebugger, InputFile, InputPort, InputPortConsole, InputPortControlConsole, InputPortMultiple, InterruptInfo, MemoryContents, MemoryHandler, MutableMemory, OutputFile, OutputPort, PortID, Register, Regs, StateWatcher, Z80System}
import org.kr.scala.z80.utils.Z80Utils

import java.nio.file.{Files, OpenOption, Path, StandardOpenOption}
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source
import scala.swing.event.Key
import scala.swing.event.Key.{Modifier, Modifiers}
import scala.util.{Failure, Success, Try}

class Simulator(val video:VideoMemory,val waitMs:Int,val tapFile:String, saveTapPrefix:String, saveTapTimestamp:Boolean) {
  private val CONTROL_PORT = PortID(0xF7)
  private val DATA_PORT = PortID(0xF5)
  private val DATA_PORT_BINARY = PortID(0xF3)

  implicit val debugger: Debugger = ZXConsoleDebugger
  //implicit val debugger: Debugger = DummyDebugger
  //implicit val debugger: Debugger = ConsoleDetailedDebugger
  implicit val memoryHandler: MemoryHandler = new MutableZXMemoryHandler(video)
  private val memory=prepareMemory
  val inputPort=new InputPortZXKey
  private val initSystem=new Z80System(memory,Register.blank,prepareOutput,prepareInput(inputPort),0,StrictCyclicInterrupt(waitMs))

  import ExecutionContext.Implicits._
  Future(StateWatcher[Z80System](initSystem) >>== Z80System.run(debugger)(Long.MaxValue))

  private def prepareMemory(implicit memoryHandler: MemoryHandler): MemoryContents =
    (StateWatcher(memoryHandler.blank(0x10000)) >>==
      memoryHandler.loadHexLines(readHexFile) >>==
      memoryHandler.lockTo(0x4000))
      .state

  private def readHexFile: List[String] =
    Source.fromResource("zx82_rom_KR_mod08_simpleIO.hex").getLines().toList

  private def prepareInput(port:InputPort): InputFile = {
    val tapFileContent=readTapFile
    val dataPort = new InputPortMultiple(tapFileContent)
    val controlPort = new InputPortMultiple(List.fill(tapFileContent.size)(1))
    InputFile.blank
      .attachPort(PortID(0xFE),port)
      .attachPort(Simulator.DATA_PORT,dataPort)
      .attachPort(Simulator.CONTROL_PORT,controlPort)
  }

  private def prepareOutput: OutputFile =
    new OutputFile(Map(
      PortID(0xFE)->new ZXOutputPort(video),
      DATA_PORT_BINARY->new ZXOutputFilePort(saveTapPrefix,saveTapTimestamp)
    ))

  private def readTapFile:List[Int]=
    Try[List[Int]]{Files.readAllBytes(Path.of(tapFile)).map(b=>Z80Utils.add8bit(b,0)).toList} match {
      case Success(list)=>
        println(f"loadad ${list.size} bytes from file: $tapFile ")
        list
      case Failure(exception)=>
        if(tapFile!="") println(f"no TAP file was loaded (${exception.toString})")
        List()
    }
}

object Simulator {
  val CONTROL_PORT: PortID = PortID(0xF7)
  val DATA_PORT: PortID = PortID(0xF5)
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

// ZX has a universal IO port: 0xFE. Outputting bits 0-2 sets border color
class ZXOutputPort(videoMemory: VideoMemory) extends OutputPort(Vector()) {
  override def put(value: Int): ZXOutputPort = {
    videoMemory.setColor(value)
    this
  }
  override val size:Int=0
  override def apply(pos:Int):Int=0
}

class ZXOutputFilePort(prefix:String,addTimestamp:Boolean) extends OutputPort(Vector()) {
  private val fileName:String = prefix +
    (if(addTimestamp) DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS").format(LocalDateTime.now)
    else "")
  private val file:Path=Path.of(fileName)
  private def ensureFileExists():Unit = if(!Files.exists(file)) Files.write(file,Array[Byte](),StandardOpenOption.CREATE)
  private def write(byteValue:Byte):Unit = {
    ensureFileExists()
    Files.write(file,Array(byteValue),StandardOpenOption.APPEND)
  }
  override def put(value: Int): ZXOutputFilePort = {
    write(Z80Utils.add8bit(value,0).toByte)
    this
  }
  override val size: Int = 0
  override def apply(pos: Int): Int = 0
}

object ZXConsoleDebugger extends Debugger {
  override def info[Watched](before:Watched,after:Watched):Unit= {
    after match {
      // output only to data port
      case outFile:OutputFile if outFile.lastPort==Simulator.DATA_PORT => print(outFile.lastValue.toChar)
      case _ =>
    }
  }
}

case class StrictCyclicInterrupt(toGo:Long,lastTCycles:Long,lastTick:Long,waitMs:Int) extends InterruptInfo {
  // trigger only if interrupts are enabled
  override def trigger(system:Z80System):Boolean = system.getRegValue(Regs.IFF)==1 && toGo<0

  // always cycle - even if interrupts are disabled (normally interrupts are generated externally to the system)
  override def refresh(system: Z80System): StrictCyclicInterrupt = {
    val step = system.elapsedTCycles - lastTCycles
    val (newToGo,newTick) = toGo match {
      case negativeToGo if negativeToGo<0 =>
        System.currentTimeMillis() - lastTick  match {
          case waitMillis if waitMillis < waitMs && waitMillis>0 =>
            Thread.sleep(waitMs-waitMillis)
          case _ =>
        }
        (toGo - step + StrictCyclicInterrupt.TCYCLES,System.currentTimeMillis())
      case otherToGo => (otherToGo - step,lastTick)
    }
    StrictCyclicInterrupt(newToGo, system.elapsedTCycles, newTick, waitMs)
  }
}

object StrictCyclicInterrupt {
  val TCYCLES:Long=Z80System.REFERENCE_CYCLES_20ms

  def apply(waitMs:Int):StrictCyclicInterrupt = new StrictCyclicInterrupt(TCYCLES,TCYCLES,System.currentTimeMillis(),waitMs)
}