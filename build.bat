@echo off
REM Build script for Nimbus AI-PSS Version with separated components on Windows

REM Create directories if they don't exist
if not exist build mkdir build
if not exist lib mkdir lib
if not exist java mkdir java

REM Move Java source files to java directory if they are in the current directory
for %%F in (Plotter.java Salter.java Smoother.java Main.java) do (
    if exist %%F (
        move %%F java\
    )
)

REM Download dependencies if they don't exist
if not exist lib\jfreechart-1.5.3.jar (
    echo Downloading JFreeChart...
    powershell -Command "Invoke-WebRequest -Uri https://repo1.maven.org/maven2/org/jfree/jfreechart/1.5.3/jfreechart-1.5.3.jar -OutFile lib\jfreechart-1.5.3.jar"
)

if not exist lib\jcommon-1.0.23.jar (
    echo Downloading JCommon...
    powershell -Command "Invoke-WebRequest -Uri https://repo1.maven.org/maven2/org/jfree/jcommon/1.0.23/jcommon-1.0.23.jar -OutFile lib\jcommon-1.0.23.jar"
)

REM Compile the Java classes
echo Compiling Java code...
javac -cp "lib\jfreechart-1.5.3.jar;lib\jcommon-1.0.23.jar;." java\*.java -d build

REM Create a JAR file
echo Creating JAR file...
cd build
jar cfe ..\lib\NimbusAI-PSS.jar Main *.class
cd ..

REM Test the JAR file with a sample CSV
if exist sample.csv (
    echo Testing with sample.csv...
    if not exist test_output mkdir test_output
    java -jar lib\NimbusAI-PSS.jar sample.csv test_output test_output\processed_data.csv
    echo Check test_output directory for results.
) else (
    echo No sample.csv found. Create one to test.
)

echo Build completed. The NimbusAI-PSS.jar is in the lib directory.
pause