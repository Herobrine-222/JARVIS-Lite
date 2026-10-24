# Migração offline de ciência quântica

## Fonte migrada

O arquivo `app/src/main/assets/quantum/caderno_unificado_ciencia_quantica_volume1.md` contém o Caderno Unificado de Ciência Quântica — Volume I enviado como fonte para esta migração. O conteúdo completo é empacotado no APK como recurso local; não precisa ser baixado durante o uso.

## Fluxo de consulta

1. `JarvisBrain` continua processando primeiro os comandos e as respostas locais já existentes.
2. Quando a base curta não encontra uma resposta, `JarvisKnowledgeBase.answer(Context, String)` consulta `JarvisQuantumKnowledgeBase`.
3. O recuperador separa o documento por títulos e parágrafos, pontua correspondências de palavras e retorna até três passagens relevantes, limitadas a cerca de 2.200 caracteres.
4. Se não encontrar correspondência suficiente ou não conseguir ler o recurso, devolve `null`. O fluxo de resposta já existente do JARVIS continua funcionando.

A inicialização em `MainActivity.onCreate` fornece o contexto da aplicação ao recuperador. O contexto de aplicação é guardado em vez de uma Activity, para não reter a tela aberta.

## Garantias de escopo

- Funciona offline, lendo somente um arquivo incluído em `assets`.
- Não adiciona permissões Android, bibliotecas externas, chamadas de rede nem telemetria.
- Não altera o esquema nem os dados da memória SQLite.
- Mantém as respostas já cadastradas em `JarvisKnowledgeBase` com prioridade.
- Não altera layout, navegação, áudio, microfone, sessão, nem funções do dispositivo.

## Limitações conhecidas

Esta etapa migra o texto integral e cria recuperação lexical de passagens. Isso não transforma o JARVIS em um modelo de linguagem nem implementa um solucionador geral de equações da mecânica quântica. O recuperador pode não reconhecer sinônimos ou perguntas muito diferentes da terminologia da fonte. As respostas devem ser entendidas como trechos recuperados do caderno, não como uma derivação nova.

## Roteiro de validação no aparelho

- Perguntar: “O que é a função de onda?”
- Perguntar: “Explique a regra de Born.”
- Perguntar: “O que é a dupla fenda?”
- Perguntar: “O que é emaranhamento quântico?”
- Perguntar: “Como funciona o tunelamento quântico?”
- Perguntar: “O que é spin?”
- Confirmar que uma consulta científica funciona com a internet do aparelho desligada.
- Confirmar que “qual é o planeta vermelho?” ainda usa a resposta curta original.
- Confirmar que comandos de horário, memória e encerramento de sessão continuam a ser tratados antes da base científica.

A compilação e o teste real no Android devem ser confirmados separadamente; a existência desses passos no roteiro não significa que já tenham sido executados.
