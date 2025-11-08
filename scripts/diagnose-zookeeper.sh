#!/bin/bash

echo "========================================="
echo "  Diagnóstico do Zookeeper"
echo "========================================="

echo ""
echo "1. Status do Container:"
docker ps -a | grep zookeeper

echo ""
echo "2. Health Status:"
docker inspect esocial-zookeeper --format='{{.State.Health.Status}}' 2>/dev/null || echo "Container não encontrado"

echo ""
echo "3. Últimos 30 logs:"
docker logs --tail=30 esocial-zookeeper 2>/dev/null || echo "Não foi possível obter logs"

echo ""
echo "4. Testando conexão na porta 2181:"
docker exec esocial-zookeeper bash -c "echo ruok | nc localhost 2181" 2>/dev/null || echo "Falhou ao conectar"

echo ""
echo "5. Verificando portas em uso:"
netstat -tuln 2>/dev/null | grep 2181 || netstat -ano | findstr :2181

echo ""
echo "========================================="

