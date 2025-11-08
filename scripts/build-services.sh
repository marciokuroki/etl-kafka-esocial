#!/bin/bash
# build-services.sh
# Script para compilar e buildar serviços

set -e  # Para em caso de erro

echo "========================================="
echo "  Build dos Serviços Spring Boot"
echo "========================================="

# Producer Service
echo ""
echo "1. Compilando Producer Service..."
cd producer-service
mvn clean package -DskipTests
echo "✓ Producer Service compilado"
cd ..

# Consumer Service
echo ""
echo "2. Compilando Consumer Service..."
cd consumer-service
mvn clean package -DskipTests
echo "✓ Consumer Service compilado"
cd ..

echo ""
echo "========================================="
echo "  ✓ Build Concluído!"
echo "========================================="


