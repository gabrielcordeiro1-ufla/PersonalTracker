## D-01 — Linguagem de Programação
- Data: 27/08/2026
- Decisão: Java como linguagem principal de desenvolvimento.
- Alternativas consideradas: Kotlin; uso misto de Java e Kotlin.
- Motivo: Os contratos e testes de base do projeto já estão em Java. Além disso, a linguagem torna as threads, executores e a sincronização explícitos, o que é uma vantagem para a análise e avaliação na camada de tempo real e concorrência.
- Consequências: O código será mais verboso. Exigirá a tradução mental de exemplos da documentação atual do Android (que é focada em Kotlin) e poderá demandar um invólucro futuramente para a interface do runtime de inferência local.
- Decidido por: Gabriel e Luís Otávio, em 27/08.

## D-02 — Tecnologia de Interface
- Data: 15/09/2026
- Decisão: Views com ViewBinding.
- Alternativas consideradas: Jetpack Compose.
- Motivo: O layout em XML é fácil de revisar em pull request e não exige nenhuma configuração adicional ou plugins extras no build. Além disso, é a tecnologia nativa mais alinhada com o uso do Java.
- Consequências: Exigirá mais código e adaptadores manuais para atualizar as listas de eventos que mudam sozinhas.
- Decidido por: Gabriel e Luís Otávio, em 15/09.

## D-03 — Concorrência na Camada de Tempo Real
- Data: 15/09/2026
- Decisão: Thread e ExecutorService em todas as camadas.
- Alternativas consideradas: Corrotinas (Kotlin).
- Motivo: Como o projeto será em Java, usaremos as primitivas nativas. A principal vantagem é a relação um para um entre tarefa e thread, o que torna a tabela de tarefas direta e alinhada à análise de escalonabilidade (Rate Monotonic) exigida na AV2.
- Consequências: O código será mais extenso, o tempo limite exigirá o uso de `Future` com `get`, e o cancelamento das tarefas será mais trabalhoso.
- Decidido por: Gabriel e Luís Otávio, em 15/09.

## D-04 — Persistência dos Eventos Cifrados
- Data: 15/09/2026
- Decisão: SQLiteOpenHelper.
- Alternativas consideradas: Room; Arquivo cifrado em anexação.
- Motivo: Possui zero dependência, funciona igualmente bem em Java e nos dá controle total para cifrar apenas os campos sensíveis individualmente com a nossa própria implementação do `AesGcmVault`, que é exatamente o que a rubrica da disciplina avalia.
- Consequências: O código será mais verboso e não haverá verificação de erros de digitação nas consultas SQL em tempo de compilação.
- Decidido por: Gabriel e Luís Otávio, em 15/09.

## D-05 — Serialização
- Data: 15/09/2026
- Decisão: org.json.
- Alternativas consideradas: Gson; kotlinx.serialization.
- Motivo: A biblioteca já vem embutida no Android e o próprio projeto base de testes já a disponibiliza. Como o objeto JSON da chamada de ferramenta tem poucos campos, bibliotecas baseadas em reflexão não trazem vantagem nesta escala.
- Consequências: A extração e montagem do JSON exigirão manipulação manual das chaves e valores.
- Decidido por: Gabriel e Luís Otávio, em 15/09.

## D-06 — Ambição quanto ao Modelo Local
- Data: 15/09/2026
- Decisão: RuleEngine (obrigatório) e LocalEngine em pelo menos um aparelho da dupla.
- Alternativas consideradas: Apenas RuleEngine.
- Motivo: O LocalEngine é recomendado para que a comparação entre os dois motores exista e o experimento final da AV2 (Lei de Amdahl) tenha uma base de validação cega sobre o que o modelo realmente agregou em desempenho.
- Consequências: Exigirá a implementação de um invólucro para consumir o motor de inferência.
- Decidido por: Gabriel e Luís Otávio, em 15/09.

## D-07 — Nível Mínimo de API
- Data: 15/09/2026
- Decisão: minSdk 26 (Android 8.0).
- Alternativas consideradas: API 24; API 29 ou mais.
- Motivo: É a recomendação oficial do docente. Abaixar para 24 faria o projeto perder a acurácia de velocidade informada pelo sensor de GPS, que é a base para a covariância exigida na semana 2.
- Consequências: Exigirá verificação condicional em tempo de execução para os tipos de serviço em primeiro plano.
- Decidido por: Gabriel e Luís Otávio, em 15/09.

## D-08 — Divisão do Trabalho
- Data: 15/09/2026
- Decisão: Alternado por semana.
- Alternativas consideradas: Divisão por arquivo; Programação em par.
- Motivo: É o arranjo que melhor equilibra a contribuição atribuível no Git e o domínio da base de código. Um integrante implementa e o outro revisa criando os testes adicionais, trocando os papéis na semana seguinte. Isso prepara os dois para a arguição individual.
- Consequências: Exigirá disciplina constante com os prazos semanais para não travar o desenvolvimento do colega nem disfarçar uma divisão por arquivo.
- Decidido por: Gabriel e Luís Otávio, em 15/09.

## D-09 — Estrutura do Trajeto (Track)
- Data: 07/10/2026
- Decisão: Track como buffer circular de capacidade fixa (padrão de 1024 fixes). Comprimento e velocidade média mantidos por acumuladores.
- Alternativas consideradas: ArrayList com crescimento ilimitado; Deque/LinkedList com descarte manual.
- Motivo: A capacidade 1 exige memória constante e consultas em tempo linear. Com o buffer circular, `addFix`, `getLength` e `getMeanSpeed` são O(1), e `slice` e `getFixes` são O(capacidade). A versão com ArrayList crescia sem limite e não atendia ao critério.
- Consequências: Fixes antigos são descartados ao encher o buffer. O comprimento continua contando a distância dos fixes descartados, mas o `slice` só enxerga o que ainda está guardado. A capacidade padrão pode ser ajustada depois. A sincronização (região crítica) fica para a capacidade 3; até lá, as consultas já devolvem cópias.
- Decidido por: Gabriel e Luís Otávio, em 07/10.

## D-10 — Política para Fixes Inválidos ou Fora de Ordem
- Data: 07/10/2026
- Decisão: O Track rejeita fix nulo e fix com timestamp anterior ao último, lançando InvalidTrackException (fail-fast). Timestamps iguais são aceitos.
- Alternativas consideradas: Ignorar silenciosamente o fix inválido; reordenar os fixes ao inserir.
- Motivo: A reconciliação (capacidade 2) assume sequência cronológica e dt maior ou igual a zero. Ignorar em silêncio esconderia falhas do sensor, e reordenar custaria mais do que O(1) por inserção.
- Consequências: O serviço de aquisição (capacidade 4) deve capturar a exceção sem derrubar a coleta e contar a ocorrência. O reconciliador precisa tratar dt igual a zero.
- Decidido por: Gabriel e Luís Otávio, em 07/10.

## D-11 — Fix com Acurácia de Velocidade e Rumo
- Data: 07/10/2026
- Decisão: Fix passa a guardar `speedAccuracy` (m/s) e `bearing` (graus, em [0, 360)), além da acurácia horizontal. Construtor com 7 parâmetros.
- Alternativas consideradas: Manter só a acurácia horizontal e arbitrar a covariância da velocidade; usar apenas a velocidade escalar, sem rumo.
- Motivo: O critério da capacidade 2 exige covariância montada com a acurácia que o sensor informa, não com valor arbitrado. A acurácia de velocidade é o motivo do minSdk 26 (D-07). O rumo permite decompor a velocidade em componentes leste e norte, mantendo a restrição de reconciliação linear nas medidas.
- Consequências: O FixSource e o leitor de registros precisam preencher os dois campos. O formato GPX não traz a acurácia de velocidade diretamente, então a fonte desses valores nos ensaios reproduzíveis (capacidades 2 e 5) deve ser definida e registrada aqui. O FixTest foi reescrito para o novo construtor.
- Decidido por: Gabriel e Luís Otávio, em 07/10.

## D-12 — Place, Event e GeoMath no Domínio Mínimo
- Data: 07/10/2026
- Decisão: Incluir Place (círculo com centro e raio), Event (tipo, instante, posição e detalhe; tipos GROSS_ERROR, STOP e SPEED_EXCEEDED) e GeoMath com distância pela fórmula de Haversine, todos em Java puro.
- Alternativas consideradas: Deixar Place e Event para semanas posteriores; Place como polígono; distância euclidiana sobre graus.
- Motivo: A proposta declara Fix, Track, Place e Event na capacidade 1, e a ficha aprovada vale como contrato. Haversine sem dependência de Android roda nos testes de unidade na JVM, e a distância euclidiana sobre graus distorce a longitude.
- Consequências: O conjunto de tipos de Event pode mudar com nova entrada neste registro. Place pode ganhar campos (como contagem de visitas) na capacidade 7.
- Decidido por: Gabriel e Luís Otávio, em 07/10.

## D-13 — Estratégia de Testes dos Contratos
- Data: 07/10/2026
- Decisão: Remover ContractsTest e ExampleUnitTest. Os testes passam a ser escritos por capacidade, contra as implementações reais.
- Alternativas consideradas: Manter o ContractsTest falhando como etapa de TDD.
- Motivo: O ContractsTest verificava lambdas criadas dentro do próprio teste, então nunca ficaria verde ao implementar as classes reais, e deixava a integração contínua vermelha na linha principal.
- Consequências: As interfaces FixSource, Reconciler e Agent ficam sem teste próprio até a capacidade correspondente. A bateria de testes de cada capacidade segue o rigor do Anexo D.2.
- Decidido por: Gabriel e Luís Otávio, em 07/10.

## D-14 — Modelo Cinemático da Reconciliação
- Data: 07/10/2026
- Decisão: Restrição de reconciliação pela regra do trapézio, por eixo (leste e norte) e por intervalo: x[k+1] − x[k] − (v[k] + v[k+1])/2 · dt = 0. Variáveis por eixo: posições e componentes da velocidade (velocidade e rumo do Fix decompostos em leste/norte).
- Alternativas consideradas: Restrição só com distância escalar entre fixes (não linear); reconciliar apenas a posição, sem usar a velocidade.
- Motivo: A restrição é linear nas medidas, como o critério da capacidade 2 exige, e usa a velocidade informada pelo sensor, que é mais precisa que a posição.
- Consequências: A regra do trapézio é aproximação, adequada a amostragem de ~1 Hz. A janela precisa de pelo menos 2 fixes. Erros de velocidade só aparecem na restrição quando a posição é precisa em relação a velocidade × dt; isso vai para a seção de limitações do relatório.
- Decidido por: Gabriel e Luís Otávio, em 07/10.

## D-15 — Covariância a partir da Acurácia do Sensor
- Data: 07/10/2026
- Decisão: Covariância diagonal. Desvio da posição por eixo = acurácia horizontal × 0,66 (o Android define a acurácia como raio de 68%). Desvio de cada componente da velocidade = speedAccuracy. Piso de 0,01 para evitar variância nula. Uma razão de otimismo, medida com o aparelho parado, pode multiplicar os desvios.
- Alternativas consideradas: Valor fixo arbitrado para a variância; usar a acurácia diretamente como desvio por eixo.
- Motivo: O critério exige covariância montada com a acurácia que o sensor informa. O fator 0,66 vem do raio de 68% de um erro 2D isotrópico.
- Consequências: Usar speedAccuracy como desvio por componente é aproximação. Erros entre eixos e entre fixes são tratados como independentes. A razão de otimismo vem do NoiseCharacterizer (percentil 68 do erro observado dividido pela acurácia declarada).
- Decidido por: Gabriel e Luís Otávio, em 07/10.

## D-16 — Detecção de Erro Grosseiro
- Data: 07/10/2026
- Decisão: Resíduo normalizado z = r / sqrt((A V Aᵀ)ᵢᵢ) com limiar 3,5. Ao disparar, identifica o fix culpado testando a remoção de cada um e escolhendo o que mais reduz o qui-quadrado total. Repete até passar no teste, com no máximo 3 remoções por janela e mínimo de 3 fixes restantes.
- Alternativas consideradas: Limiar 3,0; teste global de qui-quadrado sem identificar o fix; identificar o fix pelos resíduos vizinhos.
- Motivo: Em simulação com semente fixa (200 janelas limpas de 30 fixes), o limiar 3,0 gerou alarme falso em ~17% das janelas, e o 3,5 em ~2%, detectando em 100% um salto de 40 m. A remoção com qui-quadrado funciona igual para fixes do meio e das pontas.
- Consequências: Com sensor otimista o teste dispara em fixes bons; a razão de otimismo deve ser calibrada antes. O limiar será revisto na tabela de sensibilidade da capacidade 5, com dados reais.
- Decidido por: Gabriel e Luís Otávio, em 07/10.

## D-17 — Contrato Reconciler com Resíduo
- Data: 07/10/2026
- Decisão: A interface Reconciler ganha o método residual(y, a), que devolve A·y. A implementação é WeightedLeastSquaresReconciler, que também expõe a covariância do resíduo e o qui-quadrado.
- Alternativas consideradas: Calcular o resíduo fora da interface; criar uma interface separada.
- Motivo: Resíduo antes e depois é a evidência central da capacidade 2, e o detector depende dele.
- Consequências: Qualquer nova implementação de Reconciler deve implementar os dois métodos.
- Decidido por: Gabriel e Luís Otávio, em 07/10.

## D-18 — Projeção Local Leste/Norte
- Data: 07/10/2026
- Decisão: Projeção equiretangular em torno do primeiro fix da janela (GeoMath.eastMeters, northMeters e as inversas). Haversine continua para distâncias.
- Alternativas consideradas: Reconciliar em graus; projeção UTM.
- Motivo: Para janelas de poucas centenas de metros a diferença para o Haversine fica abaixo de 0,1 m, e a implementação é simples e testável na JVM.
- Consequências: Não vale para janelas muito extensas ou perto dos polos.
- Decidido por: Gabriel e Luís Otávio, em 07/10.

## D-19 — Leitor de Registros: CSV do GPSLogger
- Data: 11/10/2026
- Decisão: O leitor de registros lê o CSV do GPSLogger (GpsLoggerCsvReader), pelo nome das colunas, e não o GPX. Só entram pontos do provedor "gps"; rumo ou velocidade ausentes viram 0; linhas inválidas ou fora de ordem são descartadas e contadas.
- Alternativas consideradas: Ler o GPX; gravar os trajetos só com o app próprio.
- Motivo: O GPX do GPSLogger não traz acurácia, velocidade nem rumo, e o CSV traz. O Android não informa rumo com o aparelho parado.
- Consequências: O CSV não traz a acurácia de velocidade (ver D-20). A troca do GpxReader do Anexo D por este leitor é permitida desde que registrada.
- Decidido por: Gabriel e Luís Otávio, em 11/10.

## D-20 — Acurácia de Velocidade Estimada
- Data: 11/10/2026
- Decisão: Acurácia de velocidade (speedAccuracy) = desvio-padrão das diferenças de velocidade entre fixes consecutivos nas caminhadas, dividido por raiz de 2 (0,114 m/s).
- Alternativas consideradas: Calibrar com o aparelho parado; valor arbitrado; usar só o app próprio.
- Motivo: Parado, a velocidade vem sempre 0, então a calibração daria zero. A estimativa vem dos dados, e não de um valor arbitrado.
- Consequências: É um limite superior, porque a velocidade verdadeira também varia. Nas janelas gravadas pelo AcquisitionService (capacidade 4), a acurácia lida direto do sensor substitui esta estimativa.
- Decidido por: Gabriel e Luís Otávio, em 11/10.

## D-21 — Razão de Otimismo Medida em Sessão Parada
- Data: 11/10/2026
- Decisão: Razão de otimismo da posição = raio de 68% observado / acurácia declarada média, medida com o aparelho parado ao ar livre (9,5 min, 291 leituras): 0,99. É usada como fator no desvio da posição.
- Alternativas consideradas: Sessão de 1 minuto; assumir razão 1,0 sem medir.
- Motivo: Uma sessão de 1 minuto subestima o erro, porque o erro do GPS varia devagar (autocorrelação 0,99 entre pontos seguidos).
- Consequências: Vale para o ambiente da sessão; em outro ambiente deve ser remedida. Nas caminhadas o qui² por grau de liberdade ficou abaixo de 1 (mediana 0,39), indicando modelo conservador, discutido na seção de limitações do relatório.
- Decidido por: Gabriel e Luís Otávio, em 11/10.

## D-22 — Dados Reais, Taxa de Amostragem e Anonimização
- Data: 11/10/2026
- Decisão: Dados reais em src/test/resources/data: uma sessão parada de ~9,5 min e três caminhadas de ~6 min, com um ponto a cada ~2 s. As evidências usam janelas de 30 fixes. Os arquivos versionados têm a longitude deslocada por constante e horário relativo.
- Alternativas consideradas: Versionar os arquivos originais; intervalo de 1 s.
- Motivo: Com "Manter o GPS entre conexões" ligado, o aparelho entrega um ponto a cada 2 s (sem isso, a cada 8 s); 1 s não foi obtido. O repositório é público e o rastro de deslocamento é dado pessoal sensível (LGPD).
- Consequências: O intervalo de 2 s entra como limitação no relatório. O teste de 30 minutos e a aquisição a 1 Hz ficam para o AcquisitionService (capacidade 4).
  - Decidido por: Gabriel e Luís Otávio, em 11/10.

## D-23 — Primitiva de Exclusão Mútua do Buffer Compartilhado
- Data: 13/10/2026
- Decisão: CircularTrackBuffer protege o Track com um ReentrantLock (lock/unlock em try/finally). Uma única thread escreve (aquisição); várias leem (interface e agente). Toda consulta devolve cópia, e stats() lê todos os contadores sob o mesmo lock.
- Alternativas consideradas: synchronized; ReentrantReadWriteLock; CopyOnWriteArrayList; buffer sem lock (lock-free).
- Motivo: A taxa de escrita é baixa (1 Hz) e as seções críticas são curtas (add é O(1); a cópia é O(capacidade), com 1024 fixes no máximo). O ReadWriteLock acrescentaria complexidade sem ganho nessa escala. O CopyOnWriteArrayList copiaria tudo a cada escrita. A versão sem lock é incorreta (D-24 mostra isso com dados).
- Consequências: O Track nunca deve ser compartilhado diretamente entre threads. O escritor pode esperar o tempo de uma cópia feita por um leitor; esse bloqueio máximo deve ser medido na capacidade 6.
- Decidido por: Gabriel e Luís Otávio, em 13/10.

## D-24 — Método do Teste de Corrida
- Data: 13/10/2026
- Decisão: Teste de corrida com 1 escritor e 4 leitores concorrentes, buffer de 64 fixes e 30.000 escritas, executado 10 vezes e registrado uma a uma. As leituras são verificadas contra invariantes conhecidas (cópia contígua, contadores coerentes, contadores monotônicos). O mesmo teste roda também numa versão sem lock, que deve ser flagrada.
- Alternativas consideradas: Vários escritores; teste de uma execução só; testar apenas a versão com lock.
- Motivo: O escritor é único por projeto, então a corrida relevante é escrita contra leituras. Rodar o teste na versão sem lock mostra que ele detecta o defeito, e um teste que nunca falha não prova nada. O escritor cede a vez periodicamente para garantir a intercalação mesmo em máquina de poucos núcleos.
- Consequências: O teste não prova ausência de corrida em geral; só que, nessas condições, 10 execuções não encontraram violação, enquanto a versão sem lock foi flagrada em todas. Vale como limitação no relatório.
- Decidido por: Gabriel e Luís Otávio, em 13/10.

## D-25 — Estrutura da Aquisição em Tempo Real
- Data: 13/10/2026
- Decisão: O callback do GPS (HandlerThread) converte cada Location em Fix e o põe numa fila. Uma tarefa periódica de 1 s, agendada com scheduleAtFixedRate numa thread própria, esvazia a fila, entrega ao CircularTrackBuffer e mede o intervalo entre ciclos e o tempo de cada ciclo. É a única thread que escreve no buffer. A fonte de localização é o LocationManager da plataforma.
- Alternativas consideradas: Escrever no buffer direto no callback; FusedLocationProviderClient (Google Play Services); scheduleWithFixedDelay.
- Motivo: Uma tarefa periódica de fato, com período e prazo, é o que a análise de escalonabilidade (capacidade 6) exige, e mantém o escritor único (D-23). A fila impede perder fixes entre ciclos. A API da plataforma evita dependência de serviço externo. scheduleAtFixedRate mantém o período fixo.
- Consequências: O GPS pode entregar a cada 1 a 2 s (o aparelho entregou a cada 2 s com o GPSLogger), então alguns ciclos não têm fix novo; isso é contado. A tarefa nunca propaga exceção, porque o executor cancelaria a tarefa periódica em silêncio. Fixes fora de ordem são descartados e contados (D-10).
- Decidido por: Gabriel e Luís Otávio, em 13/10.

## D-26 — Serviço em Primeiro Plano, Permissões e Energia
- Data: 13/10/2026
- Decisão: Serviço em primeiro plano do tipo location, com wake lock parcial (limite de 3 h), iniciado a partir da tela visível, com START_NOT_STICKY. Permissões na ordem: localização precisa (fine e coarse juntas), notificações (Android 13+), serviço. Não se pede ACCESS_BACKGROUND_LOCATION.
- Alternativas consideradas: Pedir localização em segundo plano; serviço sem wake lock; START_STICKY.
- Motivo: Um serviço do tipo location iniciado com a tela visível continua com acesso à localização com a tela bloqueada, e a permissão em segundo plano exige o usuário ir às configurações. O wake lock mantém a CPU ligada para o período fixo. Pedir fine e coarse juntas é exigência a partir do Android 12.
- Consequências: Se o teste de 30 minutos falhar em algum fabricante, o primeiro ajuste é a otimização de bateria e o segundo, adicionar a permissão em segundo plano. O wake lock consome bateria (medida na capacidade 6).
- Decidido por: Gabriel e Luís Otávio, em 13/10.

## D-27 — Registro de Aquisição sem Coordenadas
- Data: 13/10/2026
- Decisão: O registro dos ciclos (um CSV por sessão, uma linha por ciclo) tem número do ciclo, hora, intervalo, tempo do ciclo, fixes novos, descartados, acurácias, tamanho do buffer e total, mas nenhuma coordenada. Fica no armazenamento externo do app.
- Alternativas consideradas: Gravar latitude e longitude no registro; registrar só na tela.
- Motivo: O registro só precisa provar continuidade e medir jitter. Coordenadas em claro no disco violam o requisito de segurança e privacidade (a persistência cifrada é da capacidade 9).
- Consequências: O registro não serve como base de dados de trajetos. Os trajetos para a capacidade 7 virão da persistência cifrada ou do GPSLogger. As acurácias de cada ciclo documentam a incerteza informada pelo sensor.
- Decidido por: Gabriel e Luís Otávio, em 13/10.