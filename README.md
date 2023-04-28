# zx_spectrum_scala
A ZX spectrum emulator in Scala. Uses Z80 processor simulator from [my other project](https://github.com/krzys9876/z80_sim_scala). 
As you may have heard, ZX Spectrum was extremely simplified computer so the actual simulator needs only:
* an actual processor with memory
* a dedicated I/O system for interaction with keyboard and external storage (no, I am not going to use old school tapes)
* a graphics interpreter to display the contents of pixel memory

## I/O system

ZX Spectrum made use of only one IO port: 0xFE. Different bits were responsible for keyboard scan and tape read and write.
I have rebuilt the LOAD and SAVE routines in order to make use of serial communication with my [diy replica](https://github.com/krzys9876/ZX_Spectrum_diy)
so it reused the same concept here. Only the communication is simpler - I do not need a special protocol to talk to 
ZX SIO chip since I can simply read and write bytes one by one.

For keyboard, I did not need to modify ROM, instead I read keyboard input and map keyboard codes to 5 bits of 0xFE port.
If you want to know mode about how ZX Spectrum reads the keyboard, see [this article](http://www.breakintoprogram.co.uk/hardware/computers/zx-spectrum/keyboard).

For external storage I use tap files for input and output. You can load a given file with <code>LOAD ""</code>, 
you can also save your basic program with <code>SAVE "prog_name"</code>. You cannot select files at runtime, I hope this is not a problem.

## Graphics ##

ZX Spectrum has a [non-intuitive way](http://www.breakintoprogram.co.uk/hardware/computers/zx-spectrum/screen-memory-layout) 
of holding the graphics information. In order to show the actual screen we need to read the graphic memory and
periodically draw the picture. Nowadays, since we have plenty of memory to waste I can easily replicate the part of RAM into a separate
array and show it on screen instead of sharing the memory between processor and graphics routines. I did the same in my 
[diy FPGA graphics card](https://github.com/krzys9876/ZX_Spectrum_VGA). 

The whole screen is repainted every 50ms. 

## Interrupts ##

ZX Spectrum uses one interrupt type (IM 1). It is triggered by the ULA chip every 50ms (or 60ms in the US).
I simulate it every given number of cycles, which theoretically should be an equivalent of 50ms if the simulator runs at 100%.
However, after making some performance improvements the simulator is several times faster now. If you are time-constrained (e.g. you want to play [Knight Lore](https://en.wikipedia.org/wiki/Knight_Lore)) the simulator
needs some delay. You can specify the number of milliseconds between interrupt cycles that the simulator should stick to.
I recommend ~30ms. 

NOTE: 30ms x 50 is more than a second! But keep in mind that the processor in real ZX Spectrum does not run all the time. 
It is being periodically suspended by the famous ULA chip in order for graphics chip to read the pixel memory and show
it on the TV screen. Since we do not hold the processor but sniff memory operations instead, we have to account for it
and add a few milliseconds every interrupt cycle.

## Usage ##

You can run a fat jar (created with sbt <code>assembly</code> command). Before you build this repo you need to provide
your copy of Z80 simulator. You can download it from the [simulator repo](https://github.com/krzys9876/z80_sim_scala) or
clone the code and run sbt command <code>publishLocal</code>. 

In the main project directory you will find the _run.bat_ script with some examples for Windows.
The parameters are:

     --tap-file           : a *.tap file to be loaded with LOAD instruction 
     --save-tap-prefix    : a prefix to the *.tap file that can be saved with SAVE instruction
     --save-tap-timestamp : an option to include a timestamp in the .tap file name  
     --wait-ms            : number of milliseconds required between each interrupt cycle

All parameters are optional.

## Progress log ## 

### A very basic Basic program ###
https://user-images.githubusercontent.com/41650001/224106849-dc277fd0-53cc-43e8-a75a-89d3942022b9.mp4

### Add more colors ###
https://user-images.githubusercontent.com/41650001/224249303-6dfce3de-c047-4455-982e-accfa88e3e8d.mp4

### Still a little too fast... ###
https://user-images.githubusercontent.com/41650001/224346316-99bb9405-5ccb-4104-bb56-9ded30ae6bec.mp4

### Finally I can play my game ###
https://user-images.githubusercontent.com/41650001/224388625-f2af7fd6-72ac-4ca2-8342-efc77162b471.mp4

## Copyright notice ##

ZX Spectrum ROM is copyright of Amstrad plc. According to: [this message](https://worldofspectrum.net/app/themes/wosc-classic/static/legacy/amstrad-roms.txt) it can be freely used for emulators and personal use. 

The ROM disassembly was published in 1983 in [The Complete Spectrum ROM Disassembly](https://spectrumcomputing.co.uk/entry/2000076/Book/The_Complete_Spectrum_ROM_Disassembly) so I believe it can be used 
for personal use as well.

Knight Lore is copyright of Ultimate Play the Game (see: [Wikipedia page](https://en.wikipedia.org/wiki/Knight_Lore)). It is downloadable from number of places in a form of a TAP file or as a source, but
I do not include it here.

All sample videos are my copyright and you cannot use them for any commercial purposes.
