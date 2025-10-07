ETAPA 1: 

1. Descreva, em poucas frases, o fluxo que ocorreria se usássemos HTTP síncrono entre UploadService e ThumbnailService. Que desvantagens você enxerga nesse modelo (latência, bloqueio, falhas, escalabilidade)?
Upload chama Thumbnail via REST e fica esperando. Se o Thumbnail estiver lento ou fora, trava. Em pico, a latência fica alta e o acoplamento dificulta escalar.

2. No contexto da mensageria, se o ThumbnailService estiver indisponível (por manutenção ou falha), o que você espera que aconteça com as mensagens “ImagemRecebida”? Como isso ajuda a robustez do sistema?
A “ImagemRecebida” fica na fila até o Thumbnail voltar. Nada se perde e o Upload segue aceitando uploads.

3. Qual serviço (UploadService, ThumbnailService ou FiltroService) é mais indicado para atuar como produtor (“ImagemRecebida”) e quais como consumidores? Por quê?
Produtor: UploadService publica ImagemRecebida.
Consumidores: ThumbnailService (faz miniatura) e, se quiser, FiltroService (reage depois). Produtor dispara o evento; consumidores reagem.

4. Se quisermos aplicar um filtro só para imagens de alta resolução (ex: largura > 2000px), como você estruturaria routing key / bindings para que apenas os serviços interessados recebam essas mensagens?
Crio rotas: `image.received.hd` (largura > 2000) e `image.received.sd` (≤ 2000).
- FiltroService escuta só `*.hd`.
- ThumbnailService pode ouvir tudo: `image.received.*` (ou `image.received.#` num topic).

5. Considere que ThumbnailService já gerou a miniatura e manda mensagem “ThumbnailPronta”. FiltroService quer “escutar” apenas após miniatura pronta. Como você organizaria esse fluxo com filas/exchanges para evitar disparar filtros antes de a miniatura existir?
Faço encadeamento:
Upload → publica `image.received.*` → Thumbnail gera e publica `thumbnail.ready` → FiltroService escuta `thumbnail.ready` (e ignora `image.received`).


ETAPA 3:

1. Desafio enfrentado
Qual foi a parte mais desafiadora ao declarar a configuração de exchange/queue/binding ou ao usar o RabbitTemplate para enviar a mensagem? Por quê?
Arrumar o ambiente e o scan de pacotes: Docker parado, pacote errado (main.java no package) e depois faltou o Jackson2JsonMessageConverter. Foi chato porque nada acusava “claro” na UI até ligar os logs.

2. Acerto ou insight
Qual foi a parte que você achou que funcionou muito bem — algo que “clicou” ou fez sentido na prática? Descreva esse momento.
Quando subi o RabbitConfig certo + Jackson converter, o convertAndSend(...) simplesmente funcionou. “Clicou” que o Spring já cuida da serialização e da declaração (via RabbitAdmin) se o bean estiver no pacote certo.

3. Possível falha futura
Se você fosse testar num cenário com erro (por exemplo, broker indisponível, routing key errada, fila inexistente), qual falha você acha que é mais provável de acontecer com seu código atual? Como você poderia tratar ou prevenir essa falha?
- Routing key/exchange errada → mensagem some (404 no broker) e vira 500 na API.
Como prevenir: constantes centralizadas (RabbitConfig), teste de integração que checa a existência de exchange/fila/binding, e log DEBUG do RabbitTemplate.
- Broker indisponível → exception na publicação.
Como prevenir: subir compose antes, health-check, e retry/backoff no publisher (ou retornar 503).

4. Melhoria incremental
Se você tivesse mais 10–15 minutos nessa etapa, o que você faria para melhorar ou refinar a implementação do produtor?
- Padronizar resposta do endpoint em JSON ({status, orderId}).
- Adicionar logs estruturados (orderId, routingKey).
- Pequeno teste de integração que publica e lista a fila.
- Propriedades de app separadas por perfil (dev).

5. Expectativa para a próxima etapa
Agora que você já tem o produtor funcionando, o que você quer aprender / verificar com maior atenção na etapa do consumidor (listener)? Que desafios você antecipa?
Quero ver desserialização batendo com o record, ack/nack e regra de requeue vs DLQ. Desafio: evitar loop em erro permanente e tratar concorrência (picos) sem perder mensagem.

ETAPA 4:

1. Comportamento observado
Quando você enviou uma mensagem via produtor, o método anotado com @RabbitListener foi invocado conforme esperado? Se sim, descreva o que apareceu no log. Se não, qual erro ou discrepância você observou?
Sim. Assim que dei POST, o listener rodou e apareceu no log:
Published OrderCreatedMessage: ... e depois Received OrderCreatedMessage: .... Quando o consumer estava com pacote errado, não aparecia nada na fila.

2. Desserialização e correspondência de tipos
O objeto OrderCreatedMessage foi desserializado corretamente (campos correspondendo ao JSON enviado)? Se houve erros de mapeamento, qual foi a causa provável?
Os campos bateram com o JSON. Quando faltou o Jackson2JsonMessageConverter, aí sim quebrou (tentou serialização Java).

3. Falhas potenciais no consumidor
Se dentro do método consumidor ocorrer uma exceção (por exemplo, falha de banco, erro de lógica interna), o que você espera que aconteça dadas as configurações padrão? Você conseguiu testar esse cenário? O que observou?
No modo AUTO, se eu lançar exceção, a mensagem volta pra fila (requeue padrão). Testei jogando um RuntimeException e vi o reprocesso acontecendo.

4. Controle de ack / nack e requeue
Você configurou ou pensa em configurar acknowledgment manual? Se sim, sob que condições você escolheria requeue = true ou requeue = false para mensagens problemáticas?
Se não configurou, que riscos você percebe em deixar no modo automático ou sem controle explícito?
Por enquanto deixei AUTO.
Se eu ativar MANUAL:
- requeue=true para erro transitório (ex.: timeout externo).
- requeue=false para erro permanente (ex.: payload inválido) → ideal mandar pra DLQ.

5. Escalabilidade / concorrência
Como seu consumidor reagiria se muitas mensagens chegassem de uma só vez? Você configurou concorrência ou limitará o número de threads consumidores? Que ajustes você faria para evitar sobrecarga?
Hoje está 1 consumidor simples. Se vier pico, ajusto concurrentConsumers/maxConcurrentConsumers e coloco limites de throughput. Também monitoraria tempo de processamento pra não enfileirar infinito.

6. Melhorias futuras
Se tivesse mais tempo agora, que melhorias você aplicaria no consumidor?
- Logs mais ricos (orderId, tamanho do payload).
- Métricas (contagem por status, tempo médio).
- DLQ formal + alerta.
- Idempotência básica (evitar processar duas vezes).
- Pequenos testes de integração do listener.

7. Integração com produtor / visão de fluxo completo
Pense no conjunto: produtor → broker → consumidor. Quais fragilidades você identifica no fluxo completo até agora? Que mecanismos de segurança ou robustez você adicionaria para tornar o sistema mais confiável?
Fragilidades: routing key errado some mensagem; sem DLQ pode entrar em loop; sem monitoramento, erro passa batido.
Robustez: DLQ, retries com backoff, validação do payload no consumer, observabilidade (logs/metrics/traces) e checagem de existência de exchange/queue no startup.


ETAPA 5:

1. Quais partes do sistema funcionaram exatamente como você esperava?
- POST em /orders → OrderPublisher publicou e o OrderConsumer logou o recebimento.
- Exchange/queue/binding (order.exchange → order.queue com order.created) apareceram certinho na UI.
- Com o Jackson2JsonMessageConverter, o payload bateu com os records (JSON ↔️ Java).

2. Quais dificuldades ou erros você enfrentou que precisou corrigir?
- Docker parado → Spring não conseguia falar com o broker.
- Pacote errado (main.java no package / com.exemplo fora do scan) → configs não carregavam.
- Faltou Jackson converter → 500 na publicação (falha de serialização).
- Porta 8080 ocupada → app não subia.
- Erro de sintaxe no consumer (public package) e injeção ausente no controller/publisher.

3. Se algo não funcionou, descreva o que falhou e como você diagnosticou.
- Exchange/fila não apareciam: olhei UI do Rabbit (Exchanges/Queues), liguei RabbitAdmin=DEBUG e vi que não estava declarando → ajuste de pacote/scan e beans.
- HTTP 500 no POST: chequei logs do app; indicava conversão de mensagem → adicionei Jackson2JsonMessageConverter.
- 8080 em uso: netstat -ano | findstr :8080 + taskkill no PID.
- Consumer mudo: conferi @Component, @RabbitListener, package e logs; depois validei em Consumers (1) na UI.
- Binding/roteamento: testei com curl, vi a mensagem “sumir”, conferi routing key/constantes e a seção Bindings na fila.
