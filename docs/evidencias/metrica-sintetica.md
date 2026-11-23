# Métrica de avaliação na série sintética

Evidência do caso CT-17 (`docs/testes.md`) e da métrica de sucesso definida na seção 6 de `docs/escopo.md`, registrada para o detector de linha de base (`BaselineDetector`).

## Como a série é gerada

A série sintética determinística é produzida por `GeradorSerieSintetica.gerar()` em `core/src/test/kotlin/dev/hornet/observability/core/SerieSintetica.kt`, com `java.util.Random` inicializado com semente fixa para garantir a reprodutibilidade exata das leituras. Os parâmetros padrão utilizados na avaliação são:

- Semente: `42`
- Total de leituras: `200` (índices de `0` a `199`)
- Temperatura base: `30,0 °C`
- Ruído gaussiano: desvio padrão de `0,3 °C` (`nextGaussian() * 0.3`), com cada leitura arredondada a `0,1 °C` (mesma resolução do sensor do Android)
- Picos injetados: `5` saltos de `+6 °C` sobre a base com ruído, nos índices `40`, `80`, `120`, `150` e `180`

## Como medir

A medição é executada pelo teste automatizado `AvaliacaoSinteticaTest` em `core/src/test/kotlin/dev/hornet/observability/core/AvaliacaoSinteticaTest.kt`, que passa todas as 200 leituras da série padrão pelo `BaselineDetector` com configuração padrão (janela `N = 10`, `k = 3`, piso do desvio de `0,1 °C`, limiar absoluto de `45,0 °C` e faixa válida de `0` a `60 °C`).

Para reproduzir no Linux ou no macOS:

```bash
./gradlew :core:test --info --tests dev.hornet.observability.core.AvaliacaoSinteticaTest
```

Para reproduzir no Windows:

```powershell
.\gradlew.bat :core:test --info --tests dev.hornet.observability.core.AvaliacaoSinteticaTest
```

Caso o Gradle considere a tarefa atualizada e não exiba a saída padrão do teste, adicione `--rerun-tasks` ao comando. Também é possível rodar toda a suíte do módulo `core` com `./gradlew :core:test --info` (Linux e macOS) ou `.\gradlew.bat :core:test --info` (Windows).

As duas taxas são definidas da seguinte forma:

- **Recall:** proporção dos `5` picos injetados (índices `40`, `80`, `120`, `150` e `180`) classificados como `PICO`. É calculado como `picosDetectados / 5`.
- **Taxa de falsos positivos:** proporção das leituras não injetadas após a calibração (a partir do índice `10` até o `199`, excluindo os `5` índices de pico, o que resulta em `185` leituras avaliadas) classificadas como `PICO` ou `LIMIAR_EXCEDIDO`. É calculada como `falsosPositivos / 185`. Os índices `0` a `9` ficam fora dessa conta por corresponderem à fase de calibração (`SEM_BASE`) da janela inicial de 10 leituras.

## Resultado

Na execução do teste `AvaliacaoSinteticaTest` sobre a série sintética padrão (semente `42`), a saída registrada foi:

```text
Recall: 100.00% (5 de 5 picos detectados, valor: 1.0)
Taxa de falsos positivos: 2.70% (5 de 185 leituras, valor: 0.02702702702702703)
```

Comparação com a meta definida em `docs/escopo.md`:

| Métrica | Meta | Valor medido | Situação |
|---|---|---|---|
| Recall nos picos injetados | Pelo menos 90% (`>= 0.9`) | 100,00% (5 de 5 picos, valor `1.0`) | Atingida |
| Taxa de falsos positivos (a partir do índice 10) | No máximo 5% (`<= 0.05`) | 2,70% (5 de 185 leituras, valor `0.02702702702702703`) | Atingida |

## Limitações

- **Dados sintéticos não provam eficácia em campo:** a série controlada valida o comportamento determinístico da linha de base sob ruído gaussiano e saltos conhecidos, mas não substitui medições reais de uso do aparelho.
- **Janela pequena de 10 leituras:** com `N = 10`, a média móvel e o desvio padrão são estimativas ruidosas, o que faz a taxa real de falsos positivos ficar acima do valor teórico esperado pela regra dos três desvios. Além disso, não há filtragem de ruído além do piso do desvio de `0,1 °C`.
- **Aquecimento lento coberto apenas pelo limiar de 45,0 °C:** subidas graduais entram na janela móvel e deslocam a média, passando pela regra de pico; nesses casos, o detector depende do limiar absoluto de `45,0 °C`. Em contrapartida, uma mudança permanente de patamar abaixo de `45,0 °C` continua sendo marcada como `PICO`, porque leituras anômalas não entram na janela.
- **Temperatura da bateria como indicador indireto de desempenho:** a bateria pode aquecer sem que haja thermal throttling e o throttling pode ocorrer sem que a bateria atinja os critérios de anomalia. A temperatura ambiente também não é considerada.
