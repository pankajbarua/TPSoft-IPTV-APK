@if "%DEBUG%" == "" @echo off
@rem ##########################################################################
@rem Gradle startup script for Windows
@rem ##########################################################################

set APP_BASE_NAME=%~n0
set APP_HOME=%CD%

call "%JAVA_HOME%\bin\java" -cp "%APP_HOME%\wrapper\" org.gradle.wrapper.GradleWrapperMain %*
