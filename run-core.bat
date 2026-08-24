@echo off
set JAVA_HOME=D:\Java\jdk-17
set SPRING_PROFILES_ACTIVE=dev
set PATH=%JAVA_HOME%\bin;%PATH%
D:\tools\maven\apache-maven-3.9.9\bin\mvn.cmd spring-boot:run -f outpass-core\pom.xml --no-transfer-progress > boot.log 2>&1
