# Proposta de trabalho - PersonalTracker

- **Integrantes:** Gabriel Lima Cordeiro e Luís Otávio Borges Reis
- **Data:** 27/08/2026
- **Domínio escolhido:** Deslocamento pessoal (rastreamento de posição e velocidade via GPS)

## 1. O problema
Usuários e operadores de ativos móveis precisam monitorar trajetos sem expor dados sensíveis de localização a servidores de terceiros ou nuvens comerciais. A falha atual reside no uso de rastreadores fechados que exportam dados em claro para infraestruturas externas, violando a LGPD. Na falta de uma solução soberana local, utilizam-se plataformas de mercado centralizadas que não modelam o comportamento normal do deslocamento nem operam sem conectividade.

## 2. O que é observado
- **Ativo ou processo:** O próprio deslocamento pessoal do usuário em trânsito.
- **Grandeza física medida:** Posição geográfica (latitude, longitude) e velocidade instantânea.
- **Sensor e origem da incerteza:** O receptor GNSS/GPS do dispositivo, cuja origem de incerteza é tratada através da acurácia horizontal e de velocidade informada nativamente pelas métricas do sensor (`Location.getAccuracy()`).
- **Período de amostragem pretendido e prazo associado:** Aquisição periódica a cada 1 segundo (período fixo), com prazo estrito verificado por instrumentação de tempo real.

## 3. Mapeamento das dez capacidades
| # | Capacidade | O que significa no nosso domínio | Teste de aceitação |
|---|---|---|---|
| 1 | Domínio, contratos e estruturas | As classes imutáveis de domínio (`Fix`, `Track`, `Place`, `Event`) estruturam os dados de trajeto. | O modelo de domínio valida invariantes de coordenadas geográficas e lança exceção específica; a estrutura de trajeto calcula comprimento e recorte temporal em memória constante. |
| 2 | Incerteza e reconciliação | Ajuste cinemático das leituras ruidosas de GPS utilizando a acurácia informada pelo sensor. | A covariância é montada com a acurácia do sensor; o resíduo cinemático cai em três janelas de dados reais, e um erro grosseiro injetado é detectado pelo resíduo normalizado. |
| 3 | Concorrência e região crítica | Proteção do buffer compartilhado entre a thread de aquisição de GPS e a interface gráfica/agente. | A estrutura compartilhada devolve sempre cópias defensivas; o teste de corrida concorrida executa dez vezes consecutivas sem falhas. |
| 4 | Aquisição periódica em tempo real | Serviço em primeiro plano garantindo a coleta de posições a intervalos fixos sem interrupções. | A aquisição continua operando de forma autônoma por trinta minutos contínuos com o aparelho completamente bloqueado. |
| 5 | Ensaio reprodutível e injeção de falhas | Método experimental baseado em semente determinística para injetar ruídos, saltos e perdas de pacotes. | Registro gravado reproduzido deterministicamente sob falhas injetadas, gerando a tabela de sensibilidade da detecção. |
| 6 | Instrumentação e escalonabilidade | Medição rigorosa do tempo de execução das tarefas periódicas para análise de escalonabilidade. | Tabela de tarefas com Ci medido, dispersão declarada e tempo de resposta calculado sob política de prioridade fixa. |
| 7 | Aprendizado online sob memória constante | Identificação incremental de lugares recorrentes (paradas e rotas usuais) sem acumular histórico linear. | O sistema descobre um padrão recorrente do deslocamento e prevê o próximo estado, validado em dados retidos fora do treino. |
| 8 | Agente e modelo substituível | Laço autônomo que interpreta o estado, valida saídas estruturadas e toma decisões com motor de IA desacoplado. | O agente respeita o orçamento de latência, abstém-se com evidência fraca e o sistema continua operando perfeitamente após a remoção do modelo. |
| 9 | Criptografia e frota multi-nó | Proteção de dados sensíveis em repouso e cooperação descentralizada entre nós via ponto de acesso local. | Dados cifrados com AES-GCM (IV variável e chave no Keystore); múltiplos nós sincronizam relógios (deriva medida) e fundem eventos em alerta único. |
| 10 | Validação cega, TRL e Lei de Amdahl | Avaliação comparativa rigorosa do ganho de desempenho do sistema frente à linha de base determinística. | Ensaio cego comparando motores em quatro métricas, cálculo de speedup com cinco repetições e justificativa da maturidade TRL nível a nível. |

## 4. Dados
- **Como serão obtidos:** Gravação de trajetos reais diários dos integrantes utilizando o aplicativo de código aberto GPSLogger e rotas internas no emulador do Android Studio.
- **Quantos dias ou quantas execuções:** Coleta de pelo menos 5 dias distintos de trajeto real para compor a base de validação e testes.
- **Como a separação treino e validação será congelada, e em que data:** Congelamento formal da base de dados de teste (retenção de 30% dos trajetos mais recentes) na data de entrega da AV1 (20/10/2026).

## 5. Riscos
| Risco | Probabilidade | Plano B |
|---|---|---|
| O GPS do celular perder sinal indoor ou apresentar saltos excessivos em áreas urbanas densas. | Média | Utilizar o modo de reprodução de rotas GPX pré-gravadas e o emulador do Android Studio para simular o trajeto determinístico. |
| O consumo de bateria e o gerenciador de energia do Android (Doze Mode) encerrarem o serviço em segundo plano. | Alta | Implementar o `ForegroundService` com canal de notificação persistente e política adaptativa de acordes do sistema. |
| A execução do modelo local na borda estourar a memória ou a latência permitida. | Média | Recuar totalmente para o uso exclusivo do `RuleEngine` determinístico, documentando a limitação técnica no relatório. |

## Parecer do docente
- [ ] Aprovada
- [ ] Aprovada com ressalva
- [ ] A refazer
- **Observações:**