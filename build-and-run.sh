#!/bin/bash

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${GREEN}Feedback Loops Java - Build and Run Script${NC}"
echo "=============================================="

# Check if Java is installed
if ! command -v java &> /dev/null; then
    echo -e "${RED}Java is not installed. Please install Java 17 or later.${NC}"
    exit 1
fi

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo -e "${RED}Maven is not installed. Please install Maven.${NC}"
    exit 1
fi

# Check Java version
JAVA_VERSION=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | sed '/^1\./s///' | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo -e "${RED}Java 17 or later is required. Current version: $JAVA_VERSION${NC}"
    exit 1
fi

echo -e "${YELLOW}Java version check passed: $JAVA_VERSION${NC}"

# Check if local.settings.json exists and has required values
if [ ! -f "local.settings.json" ]; then
    echo -e "${RED}local.settings.json not found. Please create it with your Azure OpenAI and Neon credentials.${NC}"
    exit 1
fi

# Check for required environment variables in local.settings.json
REQUIRED_VARS=("AzureOpenAIApiKey" "AzureOpenAIEndpoint" "AzureOpenAIEmbeddingDeploymentName" "AzureOpenAIChatCompletionDeploymentName" "NeonDatabaseConnectionString")
for var in "${REQUIRED_VARS[@]}"; do
    if ! grep -q "\"$var\":" local.settings.json || grep -q "\"$var\": \"\"" local.settings.json; then
        echo -e "${RED}$var is not set in local.settings.json${NC}"
        exit 1
    fi
done

echo -e "${YELLOW}Configuration check passed${NC}"

# Clean and compile
echo -e "${YELLOW}Cleaning and compiling project...${NC}"
mvn clean compile

if [ $? -ne 0 ]; then
    echo -e "${RED}Compilation failed. Please check the errors above.${NC}"
    exit 1
fi

echo -e "${GREEN}Compilation successful!${NC}"

# Package the functions
echo -e "${YELLOW}Packaging Azure Functions...${NC}"
mvn azure-functions:package

if [ $? -ne 0 ]; then
    echo -e "${RED}Packaging failed. Please check the errors above.${NC}"
    exit 1
fi

echo -e "${GREEN}Packaging successful!${NC}"

# Check if Azure Functions Core Tools is installed
if ! command -v func &> /dev/null; then
    echo -e "${RED}Azure Functions Core Tools is not installed.${NC}"
    echo -e "${YELLOW}Please install it from: https://learn.microsoft.com/en-us/azure/azure-functions/functions-run-local${NC}"
    exit 1
fi

echo -e "${GREEN}Starting Azure Functions locally...${NC}"
echo -e "${YELLOW}The functions will be available at:${NC}"
echo "- http://localhost:7071/api/add-podcast"
echo "- http://localhost:7071/api/update-user-history"
echo "- http://localhost:7071/api/recommend-podcasts"
echo "- http://localhost:7071/api/get-suggested-podcasts"
echo ""
echo -e "${YELLOW}Press Ctrl+C to stop the functions${NC}"
echo -e "${YELLOW}You can test the APIs using the curl_commands.sh script${NC}"
echo ""

# Run the functions
mvn azure-functions:run
