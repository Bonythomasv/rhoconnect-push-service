@echo off
set PATH=c:\Program Files (x86)\Microsoft Visual Studio 9.0\Common7\IDE;%PATH%
if "%version%" == "" (
    set version=1.0.0.2
)
set VERSION_STR=%version%

cd ANS
sed -i "s@<ApplicationVersion>\([[:digit:]]*.*[[:digit:]]\)</ApplicationVersion>@<ApplicationVersion>%VERSION_STR%</ApplicationVersion>@" Service.csproj
cd Properties
sed -i "s@\[assembly: AssemblyVersion(\"[[:digit:]]*.*[[:digit:]]\")\]@\[assembly: AssemblyVersion(\"%VERSION_STR%\")\]@" AssemblyInfo.cs
sed -i "s@\[assembly: AssemblyFileVersion(\"[[:digit:]]*.*[[:digit:]]\")\]@\[assembly: AssemblyFileVersion(\"%VERSION_STR%\")\]@" AssemblyInfo.cs
cd ..\..\
if exist "rhoconnect-push-service" rd /s /q "rhoconnect-push-service"
devenv /rebuild release ANS.sln

set message = &echo.&echo.***************************Flow Description : build.bat******************************&echo.Want to build particular version then set version parameter before running build.bat.&echo.To set version run "set version=X.X.X.X" at command line. Default version is 1.0.0.2.&echo.*************************************************************************************
echo %message%