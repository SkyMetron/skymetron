# Política de Privacidade — LGPD (Lei Geral de Proteção de Dados)

**Versão:** 1.0 — Junho 2026
**Aplicável a:** SkyMetron v0.2.0-beta e superiores
**Controladora:** SkyMetron Organização

---

## 1. Finalidade do Tratamento de Dados

O SkyMetron trata dados pessoais exclusivamente para:

- Autenticação via GitHub OAuth
- Identificação do usuário para montagem do workspace
- Armazenamento de memórias e conhecimento do usuário (Vault)
- Operação dos agentes de IA
- Melhoria contínua do serviço
- Comunicação de atualizações do software

---

## 2. Dados Coletados

### Via GitHub OAuth
- Nome de usuário do GitHub
- Endereço de e-mail público
- Foto do perfil (avatar)
- Lista de organizações que o usuário possui acesso

### Via Uso do Aplicativo
- Preferências de configuração do workspace
- API keys de provedores LLM (armazenadas localmente, nunca enviadas a servidores)
- Histórico de interações com agentes (Vault local)
- Logs de eventos do sistema (armazenamento local)
- Preferências de provedores e modos de operação

---

## 3. Armazenamento dos Dados

| Tipo de Dado | Local de Armazenamento | Criptografia |
|---|---|---|
| Token GitHub OAuth | `~/.skymetron/config.json` | AES-256 |
| API Keys de LLM | `.env` (workspace local) | Arquivo não versionado |
| Memórias e conhecimento | PostgreSQL local (pgvector) | Dados em repouso |
| Logs de eventos | Loki local | Sem transmissão externa |
| Configurações | `~/.skymetron/config.json` | AES-256 |

Nenhum dado é enviado a servidores externos, exceto:
- Chamadas aos provedores LLM configurados pelo usuário
- Consultas à API do GitHub para autenticação e identificação
- Verificação de atualizações via GitHub Releases

---

## 4. GitHub OAuth

O login via GitHub OAuth utiliza o fluxo padrão OAuth 2.0:

1. O usuário clica em "Entrar com GitHub"
2. O GitHub solicita autorização para ler perfil público e e-mail
3. Após autorização, o SkyMetron recebe um token de acesso temporário
4. O token é usado para identificar o usuário e suas organizações
5. O token é armazenado localmente em arquivo criptografado

**Escopos solicitados:**
- `read:user` — ler perfil público
- `user:email` — ler e-mail primário
- `read:org` — ler organizações do usuário

Nenhum escopo de escrita é solicitado. O SkyMetron nunca cria repositórios, faz commits ou altera dados no GitHub em nome do usuário.

---

## 5. Compartilhamento de Dados

O SkyMetron **não compartilha** dados pessoais com terceiros, exceto:

- **Provedores LLM:** O conteúdo das mensagens é enviado ao provedor selecionado pelo usuário para processamento. Cada provedor possui sua própria política de privacidade.
- **GitHub:** Dados mínimos de perfil para autenticação.
- **Atualizações:** Consulta ao GitHub Releases para verificar novas versões.

---

## 6. Direitos do Usuário (LGPD Art. 18)

O usuário tem direito a:

1. **Confirmação da existência** de tratamento de dados
2. **Acesso** aos dados tratados (exportar dados)
3. **Correção** de dados incompletos ou desatualizados
4. **Anonimização, bloqueio ou eliminação** de dados desnecessários
5. **Portabilidade** dos dados a outro fornecedor (exportar Vault)
6. **Eliminação** dos dados pessoais tratados (excluir conta local)
7. **Informação** sobre entidades públicas/privadas com quem compartilhamos dados
8. **Informação** sobre a possibilidade de não fornecer consentimento
9. **Revogação** do consentimento (revogar GitHub OAuth)

---

## 7. Exclusão de Dados

O usuário pode, a qualquer momento:

- **Exportar dados:** Gera um arquivo com todas as memórias, configurações e histórico
- **Exportar Vault:** Gera um arquivo com todo o conhecimento armazenado
- **Excluir conta local:** Remove todos os dados do workspace local
- **Limpar cache:** Remove dados temporários e logs
- **Revogar GitHub:** Remove o token de acesso do GitHub
- **Remover API Keys:** Apaga todas as chaves de provedores LLM

---

## 8. Logs e Telemetria

### Logs
- Logs do sistema são armazenados localmente via Loki
- Contêm informações de operação dos agentes, erros e métricas
- Podem conter partes de mensagens processadas
- São mantidos por 30 dias e depois rotacionados

### Telemetria
- **Desabilitada por padrão**
- Quando habilitada, coleta apenas métricas anônimas de uso (versão, provedores ativos, número de interações)
- Nunca coleta conteúdo de mensagens, API keys ou dados pessoais
- Pode ser ativada/desativada em Configurações > Privacidade

---

## 9. Segurança

- Todos os tokens e chaves são armazenados com criptografia AES-256
- O arquivo `.env` nunca é versionado
- Repositórios privados da organização nunca são expostos a usuários não autorizados
- Comunicação com GitHub é feita exclusivamente via HTTPS
- Atualizações são baixadas via GitHub Releases com verificação de checksum

---

## 10. Alterações nesta Política

Esta política pode ser atualizada periodicamente. Usuários serão notificados sobre mudanças significativas na próxima inicialização após a atualização.

---

## 11. Contato

Para exercer seus direitos LGPD ou tirar dúvidas sobre privacidade:
- **GitHub:** abrir issue em `github.com/SkyMetron/skymetron`
- **E-mail:** privacidade@skymetron.dev (quando disponível)

---

*Última atualização: Junho 2026*
