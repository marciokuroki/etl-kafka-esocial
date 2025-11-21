# Pipeline CI/CD para o Projeto

## Visão Geral

Pipeline configurado via GitHub Actions para:

- Build dos serviços Producer e Consumer
- Execução de testes unitários e integração
- Build e push de imagens Docker (opcional)
- Deploy automático via SSH para servidor remoto

## Arquivo principal

- `.github/workflows/deploy.yml`

## Como usar

- Push na branch `main` dispara pipeline.
- Possibilidade de disparar manualmente via workflow dispatch.
- Variáveis sensíveis configuradas como Secrets no GitHub: credenciais Docker, SSH.

## Benefícios

- Implantação reprodutível e controlada.
- Garantia de qualidade (testes executados).
- Entrega ágil e segura.

## Próximos passos

- Expandir testes e monitoramento.
- Automatizar notificações.
