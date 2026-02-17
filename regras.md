## Regras de implementação

Manual de Boas Práticas de Correção de Erros (Frontend Mobile + Sync Offline)1. Propósito e EscopoEste manual estabelece diretrizes claras para a correção de erros no nosso aplicativo mobile, desenvolvido em React Native/Expo, que interage com um backend Java Spring Boot e utiliza sincronização offline via SQLite. O objetivo é garantir que as correções sejam eficientes, mantenham a integridade da arquitetura do sistema e evitem a introdução de novos problemas ou a necessidade de retrabalho no backend.Nosso sistema online está funcionando de forma robusta. O foco principal deste documento é orientar a equipe na resolução de problemas específicos do ambiente mobile, especialmente aqueles relacionados ao comportamento offline e ao mecanismo de sincronização de dados. A simulação de ambiente offline, com a API indisponível (mas a rede do dispositivo funcional), é um cenário comum e crítico para nossos testes e desenvolvimento.Ao seguir estas práticas, garantimos que o aplicativo continue a oferecer uma experiência fluida e confiável, mesmo em condições de conectividade limitada, sem comprometer a estabilidade ou a performance geral do sistema.2. Regras de Ouro
Respeite o Contrato do Backend: Não altere a forma como o aplicativo interage com a API (endpoints, payloads, etc.) sem alinhamento prévio com a equipe de backend.
Priorize a Integridade dos Dados Locais: Garanta que o banco de dados SQLite local permaneça consistente e íntegro em todas as operações, online ou offline.
Mantenha a Lógica de Sincronização: Preserve a arquitetura de sincronização incremental, delta sync e a ordem de dependências (ex: OS -> Veículo -> Peça).
Evite Concorrência no SQLite: Não introduza ou aumente a concorrência de queries no SQLite. O processamento sequencial e batch writes transacionais são cruciais para evitar "database is locked".
Reproduza Antes de Corrigir: Sempre reproduza o erro em um ambiente controlado antes de tentar qualquer correção.
Colete Evidências: Obtenha logs detalhados e passos de reprodução claros para cada erro.
Pense no Impacto: Avalie o impacto de sua correção em outros módulos, especialmente no comportamento offline e na fila de sincronização.
Não Tente UPDATE sem server_id: Operações de UPDATE no servidor só devem ser tentadas se o registro local já possuir um server_id válido. Caso contrário, a operação deve ser um CREATE.
Crie Filhos Apenas com Pais Resolvidos: Não tente criar registros filhos (ex: Veículo, Peça) no servidor se o registro pai (ex: OS) ainda não tiver sido criado e seu server_id resolvido.
3. Fluxo de Correção de Erros
Reproduzir o Erro:

Siga os passos exatos para replicar o comportamento inesperado.
Teste em diferentes cenários (online, offline, transição).


Coletar Logs Detalhados:

Capture todos os logs relevantes durante a reprodução do erro.
Inclua logs de rede (endpoints, timeouts, erros), logs da fila de sync e logs do banco de dados local.


Classificar o Tipo de Erro:

Rede/API: Falha na comunicação com o backend (timeout, 4xx, 5xx).
Fila/Sync: Problemas na fila de sincronização (itens travados, ordem incorreta, backoff excessivo).
Banco Local (SQLite): Inconsistência de dados, falha na gravação/leitura, "database is locked".
UI/Estado: Problemas de renderização, estado incorreto na interface do usuário.


Analisar a Causa Raiz:

Com base nos logs e na classificação, identifique a origem do problema.
Verifique se o erro viola alguma das "Regras de Ouro".


Propor Solução Alinhada:

Desenvolva uma correção que resolva o problema sem introduzir regressões ou violar as regras de arquitetura.
Priorize soluções que utilizem os mecanismos existentes de sync e persistência local.


Testar Exaustivamente:

Realize testes unitários e de integração para a correção.
Teste o cenário original do erro e cenários adjacentes (online, offline, transição).


Documentar a Correção:

Atualize a documentação relevante, se necessário.
Prepare a descrição do Pull Request (PR) conforme o checklist.


4. Como Lidar com Erros Offline/Sync
Verifique o Status da API: Antes de qualquer coisa, confirme se a API está realmente indisponível ou se é um erro de comunicação do app.
Monitore a Fila de Sincronização: Verifique o estado dos itens pendentes, erros específicos e o comportamento de backoff.
Valide a Ordem das Dependências: Certifique-se de que as operações de CREATE e UPDATE estão respeitando a hierarquia (ex: OS antes de Veículo, Veículo antes de Peça).
Confirme a Resolução de IDs: Garanta que os localIds estão sendo corretamente mapeados para server_ids após a criação no backend.
Inspecione o Banco de Dados Local: Utilize ferramentas de debug para verificar a integridade e o conteúdo do SQLite local, especialmente para registros que deveriam ter sido sincronizados.
Atenção a "OS sem server_id": Se um item na fila de sync for um UPDATE para uma OS sem server_id, isso é um erro. A operação deveria ser um CREATE ou aguardar a criação da OS no servidor.
5. O que NÃO fazer
NÃO reescrever a arquitetura central de sincronização ou persistência offline.
NÃO alterar endpoints ou contratos da API sem discussão e aprovação da equipe de backend.
NÃO remover ou modificar a lógica de merge, delta sync ou resolução de conflitos sem entender profundamente suas implicações.
NÃO aumentar a concorrência de operações no SQLite, pois isso pode reintroduzir o problema de "database is locked".
NÃO assumir que o problema está no backend sem evidências claras nos logs do aplicativo.
NÃO enviar correções sem reprodução documentada, logs e testes adequados.
6. Checklist Obrigatório Antes de MergeAntes de abrir um Pull Request (PR) para uma correção de erro, certifique-se de que os seguintes itens foram verificados e documentados:
 Reprodução do Erro: Passos claros e concisos para reproduzir o erro.
 Logs da Reprodução: Logs completos do aplicativo durante a reprodução do erro (antes e depois da correção).
 Classificação do Erro: Indicação clara se o erro é de Rede/API, Fila/Sync, Banco Local ou UI/Estado.
 Causa Raiz: Explicação da causa raiz do problema.
 Solução Proposta: Descrição da correção e como ela se alinha às "Regras de Ouro".
 Testes:

 Testes unitários/de integração criados ou atualizados para cobrir a correção.
 Testes manuais realizados (online, offline, transição de rede).


 Impacto Avaliado: Análise de possíveis efeitos colaterais da correção.
 Estratégia de Rollback: Plano claro para reverter a mudança, se necessário.
 Mensagem do Commit/PR:

Exemplo de Commit: fix(os): Corrige 404 ao criar veículo offline devido a OS sem server_id
Exemplo de PR: [FIX] OS/Sync: Corrige falha na criação de veículos offline


7. AnexosModelo de Log EssencialAo coletar logs, garanta que as seguintes informações estejam presentes para cada operação relevante:
Timestamp: [YYYY-MM-DDTHH:MM:SS.sssZ]
Nível: [INFO], [WARN], [ERROR], [DEBUG]
Módulo: [SyncService], [OSModel], [API], [ClienteModel]
Mensagem: Descrição da operação ou erro.
Detalhes Adicionais:

Para requisições API: [API] Requesting: [METHOD] [URL], [API] Response: [STATUS] [BODY]
Para erros: [ERROR] [TipoErro]: [MensagemErro] [StackTrace]
Para sync: [SyncQueue] Processing [tipo] [ID], [SyncQueue] Item [ID] failed: [Motivo]


Modelo de Descrição de BugTítulo: [Breve descrição do problema]Módulo Afetado: [Ex: OS, Clientes, Sincronização, UI Geral]Versão do Aplicativo: [Ex: 1.2.3 (build 123)]Ambiente: [Ex: Android 13, iOS 16, Expo Go, Standalone Build]Gravidade: [Ex: Crítico, Alto, Médio, Baixo]Passos para Reproduzir:
[Passo 1]
[Passo 2]
[Passo 3]
...
Comportamento Esperado:
[Descreva o que deveria acontecer]Comportamento Observado:
[Descreva o que realmente aconteceu, incluindo mensagens de erro na tela]Logs Relevantes:
[Cole aqui os logs capturados durante a reprodução]Observações Adicionais:
[Qualquer informação extra que possa ser útil, como se o erro é intermitente, se ocorre apenas offline, etc.]
