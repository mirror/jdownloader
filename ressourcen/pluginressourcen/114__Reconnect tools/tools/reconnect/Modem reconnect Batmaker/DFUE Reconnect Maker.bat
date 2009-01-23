@echo off
title DFU Reconnect Maker (Batch Edition)

:eing
echo DFU Reconnect Maker (Batch Edition)
echo.
echo.
echo Bitte gib folgende Daten korrekt ein!
echo.
echo Gewuenschter Name der Reconnectdatei (ohne Endung)
set /p name=
echo.
echo DFU Verbindungsname
set /p vname=
echo.
echo Benutzername
set /p bname=
echo.
echo Passwort
set /p pass=
echo.
echo Deine Angaben:
echo.
echo Name der Reconnectdate: %name%
echo DFU Verbindungsname: %vname%
echo Benutzername: %bname%
echo Passwort: %pass%
echo.
:fra
echo Stimmen diese Angaben (J/N)?
set/p ant=
if %ant%==N cls
if %ant%==N goto eing
if %ant%==J goto erst
goto fra
:erst
type Data\1.txt>Reconnect\%name%.bat
echo n>>Reconnect\%name%.bat
echo rasdial "%vname%" /disconnect>>Reconnect\%name%.bat
type Data\2.txt>>Reconnect\%name%.bat
echo n>>Reconnect\%name%.bat
echo rasdial "%vname%" %bname% %pass%>>Reconnect\%name%.bat
type Data\3.txt>>Reconnect\%name%.bat
echo.
echo Deine Batch Reconnect Datei (%name%.bat) wurde im Ordner Reconnect erstellt!
echo Bitte aendere/loesche nichts in diesem Ordner!
echo.
echo Druecke eine Taste um den DFU Reconnect Maker zu beenden!
pause >nul