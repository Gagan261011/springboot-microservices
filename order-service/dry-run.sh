#!/bin/bash

################################################################################
# Spring Boot Application Dry-Run Script
################################################################################
# This script performs a diagnostic dry-run of the Spring Boot application
# without connecting to external databases or APIs.
#
# Usage:
#   ./dry-run.sh [options]
#
# Options:
#   --verify-only    Only compile and verify, don't start context
#   --help          Show this help message
################################################################################

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo -e "${BLUE}================================================================================
Spring Boot Application Dry-Run Script
================================================================================${NC}\n"

# Parse arguments
VERIFY_ONLY=false
while [[ $# -gt 0 ]]; do
    case $1 in
        --verify-only)
            VERIFY_ONLY=true
            shift
            ;;
        --help)
            echo "Usage: $0 [options]"
            echo ""
            echo "Options:"
            echo "  --verify-only    Only compile and verify, don't start context"
            echo "  --help          Show this help message"
            exit 0
            ;;
        *)
            echo -e "${RED}Unknown option: $1${NC}"
            exit 1
            ;;
    esac
done

# Step 1: Clean and compile
echo -e "${YELLOW}Step 1: Cleaning and compiling the application...${NC}"
./gradlew clean compileJava --no-daemon --console=plain
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Compilation successful${NC}\n"
else
    echo -e "${RED}✗ Compilation failed${NC}"
    exit 1
fi

# Step 2: Run verification if requested
if [ "$VERIFY_ONLY" = true ]; then
    echo -e "${YELLOW}Step 2: Running verification (skipping tests)...${NC}"
    ./gradlew build -x test --no-daemon --console=plain
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✓ Verification successful${NC}\n"
        echo -e "${GREEN}DRY-RUN (VERIFY-ONLY) COMPLETED SUCCESSFULLY${NC}"
    else
        echo -e "${RED}✗ Verification failed${NC}"
        exit 1
    fi
    exit 0
fi

# Step 3: Run dry-run utility
echo -e "${YELLOW}Step 2: Running application in dry-run mode...${NC}"
echo -e "${BLUE}--------------------------------------------------------------------------------${NC}"
./gradlew dryRun --no-daemon --console=plain

if [ $? -eq 0 ]; then
    echo -e "\n${GREEN}================================================================================
DRY-RUN COMPLETED SUCCESSFULLY
================================================================================${NC}"
else
    echo -e "\n${RED}================================================================================
DRY-RUN FAILED
================================================================================${NC}"
    exit 1
fi
