# Registro da sessão de análise do backend (28/10/2026)

Este arquivo registra os arquivos de trabalho usados para montar o documento
Deepseek/Comportamento.docx. Não é parte do projeto.

## O que foi feito
1. Lido o handoff CarHub-Handoff.md (Contexto Ajustes Futuros).
2. Lidos TODOS os 17 controllers, DTOs, enums, entidades-chave, repositórios,
   services (incluindo ServicoMecanicoService/ServicoGuinchoService completos),
   tasks, configs (SecurityConfigurations, WebConfig, application.yaml).
3. Dois subagentes produziram relatórios de conferência (abaixo), usados para
   validar e corrigir o documento final.

## Correções aplicadas após conferência dos relatórios
- Total de endpoints: 90 (não 75).
- Guincho: rotas do guincho NÃO fazem cheque de tipo (só 404; o 403 de perfil
  errado é exclusivo do mecânico).
- Duplicidade de guincho: mensagem própria ("...solicitação de guincho ativa
  para este veículo...").
- 404 de chamado de guincho: "Solicitação de guincho não encontrada."
- Redefinição de senha com e-mail inexistente → 422 "Código de verificação inválido."
- DELETE de evento com inscrições → 409 (FK Inscricao).
- PATCH de evento sem Bean Validation (validação só na persistência → 400).
- PATCH de carro: fotosCarro substitui sem limite de 2; PUT de carro não altera fotos.
- Erros de arquivo (formato/nome) em TODAS as rotas de upload → 500 com mensagem
  no corpo (RuntimeException do FileStorageService).
- Geocodificação: 422 endereço não achado / 502 chave / 504 timeout / 503 fora
  ou sem cota; cota mensal 40.000 (bloqueio desativado por padrão).
- Login 401 também quando tipoUsuarioSelecionado diverge (inclusive null).

## Relatórios dos subagentes (resumos embutidos no documento final)
- Relatório 1: serviços mecânico/guincho/painel/match/cooldown/idempotência.
- Relatório 2: serviços de domínio geral (carro, eventos, inscrições, catálogo,
  dashboard, login, perfis, arquivos, senhas, geocodificação).
