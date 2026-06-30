# Segurança — SkyMetron

## Secrets

- `.env` e `.env.*` são gitignored;
- exportações mascaram formatos conhecidos: `sk-*`, `ghp_*`, `github_pat_*`, `nvapi-*`, `gsk_*`, `AIza*`, `sk-or-*`;
- tokens e API keys não devem ser registrados em logs;
- chaves de API são fornecidas e removidas pelo usuário.

## Ações destrutivas

Ações destrutivas devem exigir confirmação. O app possui exclusão de dados locais e limpeza de cache/logs na tela de privacidade.

## Relato de vulnerabilidades

Abra uma issue privada ou entre em contato pelos canais do projeto antes de publicar detalhes exploráveis.

## Limitações beta

Criptografia uniforme de todos os arquivos locais, rotação centralizada de logs e revogação remota automática em providers ainda são pontos de melhoria.
