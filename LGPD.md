# Aviso LGPD — SkyMetron

**Versão:** 2026-06-28-v1
**Aplicável a:** SkyMetron 0.2.1-beta e superiores

## Papel do SkyMetron

SkyMetron é um app local/desktop. Na maior parte dos fluxos, os dados permanecem no computador do usuário. Quando o usuário configura GitHub ou providers externos, esses terceiros podem receber dados necessários para autenticação, processamento de prompts, respostas, pesquisa ou atualizações.

## Bases e finalidades

As finalidades incluem autenticação, execução assistida por IA, organização local do workspace, memória/vault, diagnóstico técnico, atualização automática e configuração de providers. A base prática é execução do produto solicitado pelo usuário e consentimento/aceite para fluxos que envolvem terceiros e termos legais.

## Direitos do titular

O usuário pode exportar dados locais, excluir dados locais, revogar a sessão GitHub local, remover chaves de API, limpar cache/logs e apagar workspace/vault manualmente.

## Dados sensíveis

SkyMetron não solicita intencionalmente dados pessoais sensíveis. O usuário pode, porém, inserir dados sensíveis no vault, prompts, documentos ou projetos locais. O usuário deve avaliar necessidade, base legal e risco antes de armazenar ou enviar esse conteúdo a providers.

## Transferência a terceiros

Dados podem ser enviados a GitHub e providers de IA somente quando o usuário configurar ou usar funcionalidades dependentes desses serviços. Cada terceiro possui termos e políticas próprios.

## Controles implementados nesta versão

- aceite legal versionado no app;
- termos no instalador Windows;
- exportação local com mascaramento de formatos conhecidos de secrets;
- limpeza de cache/logs locais;
- exclusão de dados locais;
- `.env` gitignored;
- bloqueio de workspace completo para usuário comum.

## Ressalvas beta

Esta versão é beta. Rotação de logs, remoção remota de tokens nos providers, classificação automática de dados sensíveis e criptografia uniforme de todos os arquivos locais permanecem como pontos de melhoria documentados na auditoria LGPD.
