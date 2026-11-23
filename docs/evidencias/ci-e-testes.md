# Evidências de CI e testes manuais

Responsável: Marcelo Almeida (QA). Este arquivo registra a execução verde do CI e os casos manuais CT-20 a CT-24 de `docs/testes.md`.

## Pipeline

- Execução verde do CI: https://github.com/celiobenj/hornet-seldon/actions/runs/45072070324
- Artefato `apk-e-relatorios`: https://github.com/celiobenj/hornet-seldon/actions/runs/45072070324/artifacts/14772978845

## Métricas de sucesso

Os valores medidos pelo teste automatizado `AvaliacaoSinteticaTest` estão registrados em [metrica-sintetica.md](metrica-sintetica.md). A meta vem da seção 6 de `docs/escopo.md`.

| Métrica | Meta | Valor medido | Situação |
|---|---|---|---|
| Recall nos picos injetados | Pelo menos 90% | 100% (5 de 5 picos) | Atingida |
| Taxa de falsos positivos | No máximo 5% | 2,70% (5 de 185 leituras) | Atingida |

Esses números vêm da série sintética determinística do módulo `core`. Não foram recalculados no emulador, porque a temperatura foi definida manualmente com `adb` e não há rótulos independentes nesse cenario.

## Testes manuais

| Caso | Resultado | Observação |
|---|---|---|
| CT-20 | SUCESSO | Sem observação adicional informada. |
| CT-21 | SUCESSO | Sem observação adicional informada. |
| CT-22 | SUCESSO | Sem observação adicional informada. |
| CT-23 | SUCESSO | Sem observação adicional informada. |
| CT-24 | SUCESSO | Sem observação adicional informada. |