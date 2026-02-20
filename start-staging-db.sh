#!/bin/bash

echo "🚀 Iniciando banco de dados de staging na porta 5433..."
docker-compose -f docker-compose-staging.yml up -d db-staging

echo "⏳ Aguardando o banco de dados ficar pronto..."
# Aguarda até o banco estar saudável
until [ "`docker inspect -f {{.State.Health.Status}} db-staging`" == "healthy" ]; do
    sleep 2;
    echo -n ".";
done

echo ""
echo "✅ Banco de dados de staging pronto e rodando em localhost:5433"
echo "Para conectar manualmente: psql -h localhost -p 5433 -U staging -d stagingdb"
echo "Senha: stagingpass"
