#!/bin/bash
# Build script for Nimbus AI-PSS Version with separated components

# Create directories if they don't exist
mkdir -p build
mkdir -p lib
mkdir -p java

# Move Java source files to java directory if they are in the current directory
for file in Plotter.java Salter.java Smoother.java Main.java; do
    if [ -f "$file" ]; then
        mv "$file" java/
    fi
done

# Download dependencies if they don't exist
if [ ! -f "lib/jfreechart-1.5.3.jar" ]; then
    echo "Downloading JFreeChart..."
    curl -L -o lib/jfreechart-1.5.3.jar https://repo1.maven.org/maven2/org/jfree/jfreechart/1.5.3/jfreechart-1.5.3.jar
fi

if [ ! -f "lib/jcommon-1.0.23.jar" ]; then
    echo "Downloading JCommon..."
    curl -L -o lib/jcommon-1.0.23.jar https://repo1.maven.org/maven2/org/jfree/jcommon/1.0.23/jcommon-1.0.23.jar
fi

# Compile the Java classes
echo "Compiling Java code..."
javac -cp "lib/jfreechart-1.5.3.jar:lib/jcommon-1.0.23.jar:." java/*.java -d build

# Create a JAR file
echo "Creating JAR file..."
cd build
jar cfe ../lib/NimbusAI-PSS.jar Main *.class
cd ..

# Test the JAR file with a sample CSV
if [ -f "sample.csv" ]; then
    echo "Testing with sample.csv..."
    mkdir -p test_output
    java -jar lib/NimbusAI-PSS.jar sample.csv test_output test_output/processed_data.csv
    echo "Check test_output directory for results."
else
    echo "No sample.csv found. Create one to test."
fi

echo "Build completed. The NimbusAI-PSS.jar is in the lib directory."