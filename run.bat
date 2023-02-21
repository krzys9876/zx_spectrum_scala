echo off
cls
java.exe -XX:+UseSerialGC -classpath .\target\scala-2.13\zx_screen-assembly-0.1.0.jar org.kr.z80.zxscreen.Main
