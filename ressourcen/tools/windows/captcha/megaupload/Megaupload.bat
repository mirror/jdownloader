set INSPATH=%~dp0
cd "%INSPATH%"
copy %1 "%INSPATH%Megaupload.gif"

symgen -i Megaupload.gif -p mu.filter -o megaupload
symseek_uf -me 2 -i megaupload.png -o Megaupload.txt -olf 0.8 -e 2 -db mu0903.db -l 4
del megaupload.png

setlocal
set variable=
set /a vn=-1
for /f "tokens=*" %%m in (megaupload.txt) do set variable=%%m
echo variable=%variable%
echo %VARIABLE:~-0,1% >letra1.txt
echo %VARIABLE:~-1,2% >letra4.txt
echo %VARIABLE:~-3,4% >resto.txt
for /f "tokens=*" %%m in (resto.txt) do set variable=%%m
echo variable=%variable%
echo %VARIABLE:~0,1% >letra2.txt
echo %VARIABLE:~1,2% >resto.txt
for /f "tokens=*" %%m in (resto.txt) do set variable=%%m
echo variable=%variable%
echo %VARIABLE:~0,1% >letra3.txt

::empiezo a comprobar letras conocidas de letra1 a letra 3
for /f "tokens=*" %%m in (letra1.txt) do set prueba=%%m
if %prueba%==0 (
@echo O>letra1.txt)
if %prueba%==1 (
@echo I>letra1.txt)
if %prueba%==2 (
@echo Z>letra1.txt)
if %prueba%==3 (
@echo B>letra1.txt)
if %prueba%==4 (
@echo A>letra1.txt)
if %prueba%==5 (
@echo S>letra1.txt)
if %prueba%==6 (
@echo G>letra1.txt)
if %prueba%==7 (
@echo T>letra1.txt)
if %prueba%==8 (
@echo B>letra1.txt)
for /f "tokens=*" %%m in (letra2.txt) do set prueba=%%m
if %prueba%==0 (
@echo O>letra2.txt)
if %prueba%==1 (
@echo I>letra2.txt)
if %prueba%==2 (
@echo Z>letra2.txt)
if %prueba%==3 (
@echo B>letra2.txt)
if %prueba%==4 (
@echo A>letra2.txt)
if %prueba%==5 (
@echo S>letra2.txt)
if %prueba%==6 (
@echo G>letra2.txt)
if %prueba%==7 (
@echo T>letra2.txt)
if %prueba%==8 (
@echo B>letra2.txt)
for /f "tokens=*" %%m in (letra3.txt) do set prueba=%%m
if %prueba%==1 (
@echo I>letra3.txt)
if %prueba%==2 (
@echo Z>letra3.txt)
if %prueba%==3 (
@echo B>letra3.txt)
if %prueba%==4 (
@echo A>letra3.txt)
if %prueba%==5 (
@echo S>letra3.txt)
if %prueba%==6 (
@echo G>letra3.txt)
if %prueba%==7 (
@echo T>letra3.txt)
if %prueba%==8 (
@echo B>letra3.txt)
if %prueba%==0 (
@echo O>letra3.txt)
for /f "tokens=*" %%m in (letra4.txt) do set prueba=%%m
if "%prueba: =%"=="A" set prueba=4
if "%prueba: =%"=="B" set prueba=8
if "%prueba: =%"=="C" set prueba=6
if "%prueba: =%"=="G" set prueba=6
if "%prueba: =%"=="I" set prueba=1
if "%prueba: =%"=="O" set prueba=0
if "%prueba: =%"=="S" set prueba=5
::junto todo denuevo
for /f "tokens=*" %%m in (letra1.txt) do set letra1=%%m
for /f "tokens=*" %%m in (letra2.txt) do set letra2=%%m
for /f "tokens=*" %%m in (letra3.txt) do set letra3=%%m
set letra4=%prueba%
set varfinal=%letra1%%letra2%%letra3%%letra4%
set varfinal=%varfinal: =%
echo %varfinal%>megaupload.txt

