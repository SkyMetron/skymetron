# LGPD Audit — SkyMetron v0.2.1-beta

**Data:** 2026-06-30
**Escopo:** LGPD, Termos, Privacidade, instalador Windows, aceite interno, logs/secrets
**Resultado:** APROVADO COM RESSALVAS

## Inventário de dados

| Dado | Onde nasce | Onde fica | Vai para terceiro? | Base/finalidade | Risco | Controle |
| ---- | ---------- | --------- | ------------------ | --------------- | ----- | -------- |
| GitHub user | GitHub OAuth | JWT/localStorage e request context | GitHub | autenticação e identificação | Médio | escopos mínimos, logout local |
| GitHub token | GitHub OAuth | memória do backend durante troca OAuth | GitHub | buscar perfil/orgs | Alto | não persistir OAuth token; JWT local separado |
| API keys | usuário | `.env`/config local | provider configurado | chamadas de IA | Alto | `.env` gitignored, export mascarado, remoção manual |
| Vault local | app/usuário | workspace/vault e banco local | se usado em prompts externos | memória/contexto local | Alto | export/delete local, responsabilidade do usuário |
| Logs | backend/desktop | logs locais/Loki/local | não por padrão | diagnóstico | Médio | clear cache/logs, mascaramento em export; rotação é ressalva |
| Traces | event bus/backend | storage local configurado | não por padrão | observabilidade | Médio | controles locais e limpeza |
| Prompts | usuário/agentes | runtime, logs/vault conforme uso | provider configurado | execução de IA | Alto | revisão humana e provider escolhido pelo usuário |
| Respostas de IA | provider/agentes | UI, vault/logs conforme uso | provider configurado | resultado assistido | Médio | revisão humana, sem garantia |
| Config local | app/backend | `~/.skymetron/config.json` | não | bootstrap, aceite, preferências | Médio | delete/export; versionamento do aceite |
| Workspace path | bootstrap | config/status local | não | localizar workspace/vault | Baixo | delete config/workspace |
| Update metadata | Electron/backend | runtime/cache | GitHub Releases | update automático | Baixo | consulta pública, confirmação de install |
| Crash/error logs | app/backend | logs locais | não por padrão | diagnóstico | Médio | mascaramento deve ser mantido; evitar secrets em exceptions |

## Fluxos auditados

### Instalação

O instalador Windows passa a usar tela de licença NSIS via `build.nsis.license = build/legal/TERMS_OF_USE.txt`. O texto inclui Termos de Uso, Privacidade e LGPD. A instalação não deve prosseguir sem aceite na página de licença.

### Primeira abertura

O app verifica token local. Sem token, mostra login GitHub. Com token, valida `/api/auth/me`, `/api/bootstrap/legal-status` e `/api/bootstrap/status`.

### Aceite dos termos

O app mantém aceite interno mesmo com aceite no instalador. O backend salva em `~/.skymetron/config.json`:

```json
{
  "termsAccepted": true,
  "lgpdAccepted": true,
  "termsVersion": "2026-06-28-v1",
  "privacyVersion": "2026-06-28-v1",
  "acceptedAt": "ISO_DATE",
  "acceptedByGitHubUser": "login ou null",
  "appVersion": "0.2.1-beta"
}
```

### Login GitHub

O backend troca o code por OAuth token, busca usuário/orgs e emite JWT local. O OAuth token não é persistido pelo fluxo atual.

### Bootstrap modo owner

João Aschenbrenner, quando membro da organização, recebe workspace de desenvolvedor. Risco: acesso a repos privados. Controle: regra restrita ao login maintainer.

### Bootstrap modo usuário comum

Usuário comum recebe workspace vazio/configurável. Controle: não expõe repos privados.

### Configuração de provider

Chaves ficam em `.env`/config local. Risco alto se o usuário compartilhar arquivos. Controle: `.env` gitignored e exportação mascarada.

### Execução de agente

Prompts podem ir a providers externos configurados. Controle: usuário escolhe provider e deve revisar conteúdo.

### Uso do vault

Vault é local, mas pode conter dados pessoais/sensíveis inseridos pelo usuário. Controle: export/delete local e aviso legal.

### Exportação de dados

`PrivacyService.exportData()` copia config, `.env` e vault. A partir desta sprint, config e `.env` são mascarados para formatos conhecidos de secrets.

### Exclusão de dados

`PrivacyService.deleteLocalAccount()` remove workspace e config local. Não revoga tokens diretamente em GitHub/providers.

### Logout

Desktop remove token/JWT, username e userType do `localStorage` na ação de revogar GitHub local.

### Update automático

Consulta GitHub Releases no boot e periodicamente. Instalação requer confirmação do usuário no app; aceite legal não é sobrescrito por update.

## Riscos LGPD

| Risco | Classificação | Controle |
| --- | --- | --- |
| Token GitHub/JWT salvo localmente | Alto | OAuth token não persistido; JWT removível por revogação local |
| API keys em `.env` | Alto | `.env` gitignored; export mascarado; remoção manual |
| Prompts enviados a providers externos | Alto | usuário escolhe provider; aviso legal; revisão humana |
| Logs contendo dados sensíveis | Médio | clear cache/logs; mascaramento em export; rotação centralizada pendente |
| Vault local contendo dados pessoais | Alto | export/delete local; responsabilidade explícita do usuário |
| Atualização automática | Baixo | GitHub Releases; confirmação de instalação |
| Modo owner puxando repos privados | Crítico | restrito a maintainer `Joao-Aschenbrenner` membro da org |
| Usuário comum criando workspace vazio | Baixo | não expõe repos privados |

## PrivacyService — validação

| Item | Status | Observação |
| --- | --- | --- |
| exportação de dados locais | Implementado | config, `.env` e vault; secrets mascarados em config/`.env` |
| delete local account | Implementado | remove workspace e `~/.skymetron` |
| clear cache | Implementado | remove `cache` |
| clear logs | Implementado | remove `logs` junto com cache |
| remove tokens | Parcial | remove config local; token desktop é removido no frontend/localStorage |
| remove provider keys | Parcial | delete local account remove `.env`; remoção individual é manual |
| revoke GitHub local session | Parcial | frontend remove JWT local; revogação remota no GitHub é manual |

## Logs e secrets

Testes cobrem mascaramento para:

- `sk-***`
- `ghp_***`
- `github_pat_***`
- `nvapi-***`
- `gsk_***`
- `AIza***`
- `sk-or-***`

Ressalva: mascaramento depende de padrões conhecidos; novos formatos de providers devem ser adicionados quando surgirem.

## Evidências esperadas

- `docs/audits/screenshots/installer-terms.png`
- `docs/audits/screenshots/app-legal-page.png`
- `docs/audits/screenshots/privacy-page.png`

## Validação do instalador Windows

Artefato gerado: `sky-desktop/release/SkyMetron Setup 0.2.1-beta.exe`.

Evidência NSIS:

- `builder-debug.yml` contém `MUI_PAGE_LICENSE "...build\legal\TERMS_OF_USE.txt"`;
- a janela do instalador mostra `Acordo de Licença` antes da instalação;
- a tela exibe `SkyMetron 0.2.1-beta - Termos de Uso, Privacidade e LGPD`;
- antes do aceite não há botão de instalação, apenas `Eu Concordo` e `Cancelar`;
- o texto da tela informa que é necessário aceitar o acordo para instalar.

## Resultado

APROVADO COM RESSALVAS

Motivo: a sprint corrige aceite legal no instalador e no app, adiciona documentos legais, versionamento e mascaramento de secrets. Ainda é beta e permanecem ressalvas em rotação centralizada de logs, revogação remota em providers, criptografia uniforme de arquivos locais e classificação automática de dados sensíveis.
