# Data Processing — SkyMetron

**Versão:** 2026-06-28-v1

| Categoria | Origem | Local | Terceiros | Finalidade | Controle |
| --- | --- | --- | --- | --- | --- |
| GitHub user/id/e-mail público | Login OAuth | JWT/localStorage e backend request context | GitHub | autenticação e tipo de workspace | revogar sessão local |
| OAuth GitHub token | GitHub OAuth | memória do backend durante login | GitHub | buscar perfil/orgs | não persistir após troca |
| JWT local | Backend auth | localStorage desktop | não | sessão local | logout/revogar GitHub |
| API keys | Usuário | `.env`/config local | provider configurado | chamadas LLM | remover `.env`, export mascarado |
| Workspace path | Bootstrap | `~/.skymetron/config.json` | não | abrir workspace local | excluir config |
| Vault local | Usuário/app | workspace/vault e banco local | provider se prompt usar conteúdo | memória e contexto | exportar/excluir |
| Prompts/respostas | Usuário/providers | memória/logs/vault conforme uso | provider configurado | execução IA | revisar antes de enviar |
| Logs/traces | App/backend | logs locais/Loki/local | não por padrão | diagnóstico | limpar cache/logs |
| Update metadata | GitHub Releases | runtime/cache | GitHub | atualização automática | desabilitar rede/adiar update |

## Minimização

Coletar apenas o necessário para autenticação, execução local e providers escolhidos pelo usuário.

## Retenção

Dados locais permanecem até exclusão pelo usuário. Logs e caches devem ser limpos pela tela de privacidade ou manualmente.
