@echo off
rem Carrega as variaveis do arquivo .env (se existir) e inicia o sistema.
cd /d "%~dp0"
if exist .env (
  for /f "usebackq eol=# tokens=1,* delims==" %%a in (".env") do set "%%a=%%b"
) else (
  echo Aviso: .env nao encontrado. A IA ficara desativada.
)
mvn spring-boot:run
