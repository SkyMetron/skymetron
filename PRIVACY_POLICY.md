# Política de Privacidade — SkyMetron

**Versão da política:** 2026-06-28-v1
**Aplicável a:** SkyMetron 0.2.1-beta e superiores

## Dados que podem ser tratados

O SkyMetron pode tratar:

- nome de usuário GitHub;
- ID do GitHub;
- e-mail público do GitHub, se disponível;
- token de autenticação OAuth durante o fluxo de login;
- token local/JWT de sessão no desktop;
- caminhos locais de workspace;
- configurações do app;
- preferências do usuário;
- logs técnicos;
- histórico local de ações;
- conteúdo do vault local;
- prompts e respostas processadas pelos providers configurados;
- chaves de API fornecidas pelo usuário;
- metadados de atualização do app.

## Dados sensíveis

O SkyMetron não solicita intencionalmente dados pessoais sensíveis. Porém o usuário pode inserir dados sensíveis no vault, prompts, documentos ou projetos locais. Nesse caso, o usuário é responsável por avaliar se deve ou não armazenar esse conteúdo.

## Onde os dados ficam

Dados locais:

- workspace;
- vault;
- logs;
- config;
- cache;
- tokens locais;
- chaves locais.

Dados enviados a terceiros:

- apenas quando o usuário configurar provider externo;
- apenas quando usar funcionalidade que dependa desse provider;
- GitHub durante autenticação, identificação e verificação de atualizações.

## Finalidade

Os dados são usados para autenticação, bootstrap de workspace, sincronização GitHub quando autorizada, execução de agentes, memória local, logs de diagnóstico, atualizações automáticas e configuração de providers.

## Direitos do titular

O usuário pode exportar dados locais, apagar dados locais, revogar login GitHub local, remover chaves de API, limpar cache, limpar logs e apagar workspace/vault manualmente.

## Retenção

Dados locais permanecem no computador do usuário até ele excluir. Tokens devem ser removidos no logout ou revogação local. Chaves de API devem poder ser removidas pelo usuário na configuração local. Logs devem ter rotação/limite; na versão beta, esse ponto permanece como ressalva técnica quando a rotação depende do ambiente local.

## Segurança

O arquivo `.env` deve permanecer gitignored. Tokens não devem ir para logs. Chaves não devem aparecer em relatórios. Exportações mascaram formatos conhecidos de secrets. Operações destrutivas exigem confirmação.

## Terceiros

Providers externos processam dados conforme seus próprios termos e políticas. Consulte [THIRD_PARTY_PROVIDERS.md](THIRD_PARTY_PROVIDERS.md).
