@echo off
setlocal ENABLEDELAYEDEXPANSION
pushd "%~dp0"

REM ================= CONFIG =================
set "SPIGOT=spigot-api-1.20.1-R0.1-SNAPSHOT.jar"
set "OUTJAR=CoinCash.jar"
set "SRCPATH=src\com\foxsrv\cash"

REM ================= PREP ===================
echo.
echo === Preparando diretorios ===
if not exist out mkdir out
if not exist out\classes mkdir out\classes
if exist out\%OUTJAR% del /q out\%OUTJAR%

REM ================= AMBIENTE =================
echo === Verificando ambiente Java ===
where javac >nul 2>nul
if errorlevel 1 (
  echo [ERRO] javac nao encontrado. Instale JDK 17 ou configure JAVA_HOME.
  pause
  popd
  exit /b 1
)

if not exist "%SPIGOT%" (
  echo [ERRO] Spigot API nao encontrada: %SPIGOT%
  pause
  popd
  exit /b 1
)

REM ================= COLETAR FONTES =================
echo.
echo === Coletando arquivos .java ===
set "FILES="

for /R "%SRCPATH%" %%F in (*.java) do (
  set "FILES=!FILES! "%%~fF""
)

if "!FILES!"=="" (
  echo [ERRO] Nenhum .java encontrado em %SRCPATH%
  pause
  popd
  exit /b 1
)

REM ================= COMPILAR =================
echo.
echo === Compilando CoinCredit ===
javac ^
  -encoding UTF-8 ^
  --release 17 ^
  -Xlint:deprecation ^
  -Xlint:unchecked ^
  -classpath ".;%SPIGOT%" ^
  -d out\classes ^
  !FILES!

if errorlevel 1 (
  echo.
  echo [ERRO] Falha na compilacao.
  pause
  popd
  exit /b 1
)

REM ================= COPIAR RESOURCES =================
echo.
echo === Copiando resources ===
if exist resources (
  xcopy /E /Y /I resources out\classes >nul
) else (
  echo [AVISO] Pasta resources nao encontrada.
)

REM ================= EMPACOTAR =================
echo.
echo === Criando JAR: out\%OUTJAR% ===

set "JARCMD="
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\jar.exe" set "JARCMD=%JAVA_HOME%\bin\jar.exe"
if not defined JARCMD where jar >nul 2>nul && set "JARCMD=jar"

if not defined JARCMD (
  echo [ERRO] comando jar nao encontrado.
  pause
  popd
  exit /b 1
)

pushd out\classes
"%JARCMD%" cf ..\%OUTJAR% *
popd

echo.
echo === BUILD FINALIZADO COM SUCESSO ===
echo Arquivo gerado: out\%OUTJAR%
echo Copie para a pasta plugins do servidor.
pause
popd
