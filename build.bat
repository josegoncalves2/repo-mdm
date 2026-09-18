@echo off
setlocal
set JAVA_HOME=C:\Users\40446686808\projetos\MDM\repo-mdm\.jdk21
cd /d "C:\Users\40446686808\projetos\MDM\repo-mdm\server-source"
call "C:\Users\40446686808\projetos\MDM\repo-mdm\.maven\bin\mvn.cmd" clean package -DskipTests
endlocal
