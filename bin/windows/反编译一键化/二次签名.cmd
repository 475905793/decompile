ECHO.�������ڻر�����...
java -jar apktool.jar b %~nx1  -o game_unsigned.apk
ECHO.    ����ǩ��.....
ECHO.
ECHO.
jarsigner -verbose -keystore cyy_game.keystore -storepass cyy1888 -signedjar game_signed_Cyy.apk game_unsigned.apk cyy_game.keystore -digestalg SHA1 -sigalg MD5withRSA

pause
pause