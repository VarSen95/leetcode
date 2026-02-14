#!/bin/bash

# Get the directory where the script is located
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Navigate to the Maven project root (4 levels up from src/test/java/practice)
PROJECT_ROOT="$SCRIPT_DIR/../../../.."

# Change to project root
cd "$PROJECT_ROOT" || exit 1

# Use a project-local Maven repository to avoid permission issues in ~/.m2
MAVEN_REPO="$PROJECT_ROOT/.m2-local"
mkdir -p "$MAVEN_REPO"
MVN_CMD="mvn -Dmaven.repo.local=$MAVEN_REPO"

# If a test file argument is provided, extract the test class name
if [ $# -gt 0 ]; then
    TEST_FILE="$1"
    # Extract just the filename without path and extension
    TEST_CLASS=$(basename "$TEST_FILE" .java)
    TEST_PACKAGE="practice"
    
    echo "Running test: $TEST_PACKAGE.$TEST_CLASS"
    echo "Project root: $PROJECT_ROOT"
    echo ""
    
    # Run the specific test class using Maven
    $MVN_CMD test -Dtest="$TEST_PACKAGE.$TEST_CLASS"
else
    # If no argument, run all tests in the practice package
    echo "Running all tests in practice package"
    echo "Project root: $PROJECT_ROOT"
    echo ""
    
    $MVN_CMD test -Dtest="practice.*"
fi
