#!/bin/bash
# Nimbus AI-PSS Version - Automated Setup Script
# This script installs all necessary dependencies and builds the project

echo "🚀 Setting up Nimbus AI-PSS Version..."

# Install Python dependencies
echo "📦 Installing Python dependencies..."
pip install flask pandas numpy werkzeug

# Create project directories
echo "📁 Creating project directories..."
mkdir -p java
mkdir -p static/css
mkdir -p static/js
mkdir -p static/visualizations
mkdir -p templates
mkdir -p uploads
mkdir -p temp
mkdir -p lib
mkdir -p build

# Run the build script to compile Java components
echo "🔨 Building Java components..."
chmod +x build.sh
./build.sh

# Create a sample CSV for testing
echo "📊 Creating a sample CSV file..."
cat > sample.csv << EOF
time,value,category
1,12.5,A
2,13.2,A
3,15.7,A
4,14.3,A
5,16.8,B
6,18.1,B
7,17.5,B
8,19.2,B
9,22.3,C
10,21.7,C
11,24.5,C
12,23.8,C
13,25.6,D
14,28.1,D
15,27.3,D
16,29.8,D
17,32.5,E
18,31.7,E
19,34.2,E
20,33.6,E
EOF

# Test the application with the sample CSV
echo "🧪 Testing the application with sample data..."
mkdir -p test_output
java -jar lib/NimbusAI-PSS.jar sample.csv test_output test_output/processed_data.csv

echo "✅ Setup complete! You can now run the application with: python app.py"