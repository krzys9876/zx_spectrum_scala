echo off
cls
rem java -jar .\target\scala-2.13\zx_screen-assembly-0.2.7.jar --wait-ms=0 --save-tap-prefix=input-files/save --save-tap-timestamp=true
rem java -jar .\target\scala-2.13\zx_screen-assembly-0.2.7.jar --wait-ms=0 --tap-file=input-files/save20230312_163112_322.tap --save-tap-prefix=input-files/save --save-tap-timestamp=true
rem java -jar .\target\scala-2.13\zx_screen-assembly-0.2.7.jar --wait-ms=0 --tap-file=input-files/sudo.tap
rem java -jar .\target\scala-2.13\zx_screen-assembly-0.2.7.jar --wait-ms=0 --tap-file=input-files/test6col.tap
java -jar .\target\scala-2.13\zx_screen-assembly-0.2.7.jar --wait-ms=30 --tap-file=input-files/KL.tap
