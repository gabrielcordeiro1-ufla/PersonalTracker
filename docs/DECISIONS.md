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