FROM python:3.11-slim

WORKDIR /app

# Instalar dependências
RUN pip install --no-cache-dir flask==3.0.0

# Copiar aplicação
COPY app.py .

# Expor porta
EXPOSE 5001

# Comando de inicialização
CMD ["python", "app.py"]
