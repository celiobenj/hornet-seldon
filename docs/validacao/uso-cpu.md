# Validação do uso da CPU no aparelho

Data: 25/11/2026.
Ambiente: emulador do Android Studio `Medium_Phone_API_37.0`, instância `emulator-5554`, API 37, 6 processadores reportados por `Runtime.getRuntime().availableProcessors()`.
Código usado na medição: app Kotlin mínimo e descartável, criado fora do repositório em `%LOCALAPPDATA%\Temp\opencode\validacao-cpu`. Nenhum arquivo do `app`, do `core` ou do contrato foi alterado para esta validação.

Objetivo: decidir se a CPU pode virar sinal do classificador mantendo o escopo fechado de linha de base térmica.

## Como a medição foi feita

O app temporário tentou ler `/proc/stat` e `/proc/loadavg` com `File(...).readText()` e enviou o conteúdo ou o erro para o Logcat com a tag `ValidacaoCPU`. A cada 5 segundos calculou o uso do próprio processo com `Process.getElapsedCpuTime()`, `SystemClock.elapsedRealtime()` e `availableProcessors()`, pela conta `deltaCpu / (deltaTempo * processadores) * 100`. A primeira amostra só estabeleceu a referência. O botão iniciou uma thread por processador, totalizando 6 threads, por 180.000 ms.

Não foi usado root, permissão especial nem alteração de configuração do emulador.

## Resultado por método

| Método | Resultado observado | Limite encontrado | Uso possível no classificador |
|---|---|---|---|
| `/proc/stat` | Leitura bloqueada. Logcat: `java.io.FileNotFoundException: /proc/stat: open failed: EACCES (Permission denied)`. Causa: `android.system.ErrnoException`, `EACCES`. | Em um app comum, sem root, o arquivo não abre. Não há CPU total nem tempo por modo de execução para calcular uso global. | Não pode ser fonte do classificador nesta fase. |
| `/proc/loadavg` | Leitura bloqueada. Logcat: `java.io.FileNotFoundException: /proc/loadavg: open failed: EACCES (Permission denied)`. Causa: `android.system.ErrnoException`, `EACCES`. | Em um app comum, sem root, o arquivo não abre. Não há média de carga do sistema. | Não pode ser fonte do classificador nesta fase. |
| `Process.getElapsedCpuTime()` | Funcionou. Ocioso ficou em 0,34% e 0,09%. Durante a carga, as amostras ficaram entre 93,29% e 98,31%. Após o fim da carga, o retorno foi 65,99% e depois 0,05%, 0,05% e 0,04%. | Mede somente o tempo de CPU acumulado do próprio processo, em todas as suas threads. Precisa de duas amostras separadas pelo intervalo. A primeira amostra teve 17,19% por causa da inicialização e deve ser descartada. Não mede outros processos nem o throttling do aparelho. | Pode ser sinal secundário e opcional, desde que calibrado e separado da regra de temperatura. |

O botão iniciou a carga às 17:42:23 e as 6 threads encerraram às 17:45:23, confirmando os 3 minutos. A tela mostrou o percentual e o estado de leitura de cada arquivo. Os valores de `/proc` apareceram como `bloqueado: FileNotFoundException`.

## Decisões possíveis

| Decisão | Quando se aplica | O que muda nas regras do classificador |
|---|---|---|
| `go` | Leitura de `/proc` liberada e percentual de CPU confiável no aparelho real. | CPU entraria como sinal oficial. Seriam necessários nova porta no contrato, novo tipo de avaliação, limiar próprio, janela própria e revisão da métrica de sucesso. |
| `go com plano B` | Leitura de `/proc` bloqueada, mas percentual do próprio processo funciona e acompanha a carga. | Regras atuais de temperatura permanecem. CPU fica como sinal suplementar fora do detector, sem alterar `SEM_BASE`, `PICO`, `LIMIAR_EXCEDIDO` nem a janela. Se a CPU estiver indisponível, o classificador continua apenas com temperatura. |
| `no-go` | Nenhum método fornece valor confiável. | Nenhuma mudança nas regras. Apenas registro da limitação. |

## Decisão tomada

`go com plano B`.

`/proc/stat` e `/proc/loadavg` falharam com `EACCES` no emulador, sem permissão especial. Não há base para usar CPU global como sinal nesta entrega. `Process.getElapsedCpuTime()` respondeu bem à carga controlada, saindo da faixa ociosa de aproximadamente 0,1% para a faixa de 93% a 98%, mas mede somente o processo do app. Isso serve para observar o esforço do próprio app, não para provar aquecimento do aparelho ou detectar carga causada por outros apps.

## Impacto nas regras do classificador

Nesta entrega, nada muda. `ConfiguracaoDetector`, `StatusLeitura`, `Avaliacao`, `AnomalyDetector`, `TemperatureSource` e `BaselineDetector` continuam como estão. A ordem de decisão segue sendo validade, limiar absoluto, janela incompleta, z-score e NORMAL. A métrica sintética continua baseada apenas em temperatura.

O plano B significa que uma issue futura pode estudar um sinal suplementar de CPU do processo, com amostragem de 5 segundos, descarte da primeira amostra, limite e quantidade de amostras sustentadas ainda a calibrar. Esse sinal deve reforçar um aviso quando a temperatura já estiver em `PICO` ou `LIMIAR_EXCEDIDO`, sem entrar na janela de temperatura e sem mudar os rótulos atuais.

Se a equipe decidir transformar CPU em sinal oficial do core, será preciso abrir o contrato, criar nova porta, definir novos status e limiares, atualizar o escopo fechado e recalcular a métrica de sucesso. Essa mudança fica fora desta validação.

## Limitação do resultado

A validação foi feita em emulador, não em aparelho físico. Um aparelho real pode ter políticas diferentes de acesso a `/proc` e comportamento térmico diferente. A conclusão vale para o ambiente testado. Antes de qualquer mudança no classificador, a equipe deve repetir a leitura no aparelho de referência e calibrar o sinal com dados rotulados.
