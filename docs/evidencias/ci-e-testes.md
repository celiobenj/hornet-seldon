# Evidencias de CI e testes manuais

Responsavel: Marcelo Almeida (QA). Este arquivo registra a execucao verde do CI e os casos manuais CT-20 a CT-24 de `docs/testes.md`.

## Pipeline

- Execucao verde do CI: https://github.com/celiobenj/hornet-seldon/actions/runs/45072070324
- Artefato `apk-e-relatorios`: https://github.com/celiobenj/hornet-seldon/actions/runs/45072070324/artifacts/14772978845

## Metricas de sucesso

Os valores medidos pelo teste automatizado `AvaliacaoSinteticaTest` estao registrados em [metrica-sintetica.md](metrica-sintetica.md). A meta vem da secao 6 de `docs/escopo.md`.

| Metrica | Meta | Valor medido | Situacao |
|---|---|---|---|
| Recall nos picos injetados | Pelo menos 90% | 100% (5 de 5 picos) | Atingida |
| Taxa de falsos positivos | No maximo 5% | 2,70% (5 de 185 leituras) | Atingida |

Esses numeros vem da serie sintetica deterministico do modulo `core`. Nao foram recalculados no emulador, porque a temperatura foi definida manualmente com `adb` e nao ha rotulos independentes nesse cenario.

## Testes manuais

| Caso | Resultado | Observacao |
|---|---|---|
| CT-20 | SUCESSO | Sem observacao adicional informada. |
| CT-21 | SUCESSO | Sem observacao adicional informada. |
| CT-22 | SUCESSO | Sem observacao adicional informada. |
| CT-23 | SUCESSO | Sem observacao adicional informada. |
| CT-24 | SUCESSO | Sem observacao adicional informada. |