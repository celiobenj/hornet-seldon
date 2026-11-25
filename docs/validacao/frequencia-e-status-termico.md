# Validação de frequência da CPU e status térmico no aparelho

Documento de validação técnica referente à Issue #40 (`[D1] Validar no aparelho a frequencia da CPU e o status termico`). O experimento verificou se um aplicativo Android comum, sem root, sem permissões especiais e sem uso de `adb shell`, consegue ler continuamente as frequências de CPU via `sysfs` e o status térmico via `PowerManager`, junto à temperatura da bateria, durante repouso e carga controlada.

Todo o código de teste foi mantido em um projeto Android descartável fora do repositório `hornet-seldon`.

## 1. Aparelho testado e metodologia

- **Modelo:** Motorola Moto G60 (codinome `hanoip`, hardware `qcom`, 8 núcleos lógicos)
- **Sistema operacional:** Android 12 (API 31)
- **Quantidade de aparelhos testados:** 1 (aparelho próprio)
- **Sessão coletada:** 86 amostras em intervalos de 5 segundos, de `12:18:10` até `12:25:19`, exportadas em CSV diretamente pelo aplicativo descartável
- **Condições de acesso:** leitura feita dentro do processo do aplicativo, sem root, sem `adb shell` e sem permissões adicionais no manifesto
- **Protocolo:**
  1. Repouso inicial: amostras `#1` a `#49` (`12:18:10` a `12:22:12`, 49 amostras).
  2. Carga controlada de CPU: amostras `#50` a `#84` (`12:22:17` a `12:25:09`, 35 amostras), com 8 threads executando cálculos contínuos em ponto flutuante por 3 minutos.
  3. Recuperação pós-carga: amostras `#85` e `#86` (`12:25:14` e `12:25:19`, 2 amostras).

## 2. Tabela de sinais testados

Todos os sinais abaixo foram lidos dentro do aplicativo, sem root e sem `adb shell`.

| Sinal | Caminho ou API | Funciona sem root | Comportamento observado no Moto G60 | Observações técnicas |
|---|---|---|---|---|
| Frequência atual da CPU | `/sys/devices/system/cpu/cpuN/cpufreq/scaling_cur_freq` (`cpu0`..`cpu7`) e `/sys/devices/system/cpu/cpufreq/policyN/scaling_cur_freq` (`policy0` para `cpu0`..`5` e `policy6` para `cpu6`..`7`) | Sim | Valor em kHz, variando ao longo do tempo (ex.: `768000 kHz` = `768,0 MHz` em repouso; `1804800 kHz` na `policy0` e `2169600` a `2304000 kHz` na `policy6` sob carga) | Sem bloqueio de permissão ou SELinux. Leituras sequenciais entre núcleos da mesma policy podem divergir levemente na mesma amostra pelos milissegundos entre leituras de arquivos distintos. |
| Teto atual da CPU | `/sys/devices/system/cpu/cpuN/cpufreq/scaling_max_freq` e `policyN/scaling_max_freq` | Sim | Manteve `1804,8 MHz` na `policy0` e `2304,0 MHz` na `policy6` (100% do teto) durante toda a sessão | Sem bloqueio de permissão ou SELinux. Houve 0 divergências entre núcleos pertencentes à mesma policy. |
| Frequência máxima de fábrica | `/sys/devices/system/cpu/cpuN/cpufreq/cpuinfo_max_freq` e `policyN/cpuinfo_max_freq` | Sim | Fixo em `1804,8 MHz` na `policy0` (`cpu0`..`5`) e `2304,0 MHz` na `policy6` (`cpu6`..`7`) | Sem bloqueio. Serve como referência estável para calcular o percentual de redução de teto (`scaling_max_freq / cpuinfo_max_freq`). |
| Status térmico do sistema | `PowerManager.getCurrentThermalStatus()` (API 29+) | Sim | Retornou `NONE` (código `0`) em todas as 86 amostras (repouso e carga) | Exige verificação de `Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q` para fallback em Android 9 ou inferior. |
| Headroom térmico (opcional) | `PowerManager.getThermalHeadroom(0)` (API 30+) | Não | Retornou `NaN` em todas as 86 amostras | Sem suporte do fabricante neste modelo. |
| Temperatura da bateria | `Intent.ACTION_BATTERY_CHANGED` (`BatteryManager.EXTRA_TEMPERATURE`) | Sim | Variou de `33,9 °C` no início do repouso para `34,1 °C` no início da carga e `34,3 °C` ao final dos 3 minutos de carga (`+0,2 °C` durante a carga) | Valor entregue pelo Android em décimos de °C, já adotado na linha de base do projeto. |

## 3. Comportamento sob carga controlada (8 threads, 3 minutos)

### Resumo por fase
- **Repouso inicial (`#1` a `#49`, `12:18:10` a `12:22:12`):** temperatura da bateria entre `33,9 °C` e `34,1 °C`, `policy0` com média de `1238,7 MHz`, `policy6` com média de `915,7 MHz`, `ThermalStatus` em `NONE`.
- **Carga controlada (`#50` a `#84`, 35 amostras):** bateria subiu de `34,1 °C` para `34,3 °C` (`+0,2 °C`). A `policy0` (`cpu0`..`5`) manteve `1804,8 MHz` (100%) em todas as 35 amostras. A `policy6` (`cpu6`..`7`) operou majoritariamente em `2169,6 MHz` (94,2%, mediana), com quedas pontuais isoladas (mínimo de `979,2 MHz` na amostra `#63`). O teto (`scaling_max_freq`) permaneceu em 100% nas duas policies e o `ThermalStatus` continuou em `NONE`.
- **Pós-carga (`#85` em diante):** queda imediata das frequências atuais (`policy0` para `768,0 MHz` e `policy6` para `979,2 MHz`), mantendo bateria em `34,3 °C` e `ThermalStatus` em `NONE`.

### Amostras representativas da sessão

| Amostra | Horário | Momento | Bateria | `policy0` atual (teto) | `policy6` atual (teto) | `ThermalStatus` | `Headroom(0)` |
|---|---|---|---|---|---|---|---|
| `#49` | `12:22:12` | Repouso antes da carga | `34,1 °C` | `1612,8 MHz` (`1804,8 MHz`) | `979,2 MHz` (`2304,0 MHz`) | `NONE` (`0`) | `NaN` |
| `#50` | `12:22:17` | Carga `0 s` | `34,1 °C` | `1804,8 MHz` (`1804,8 MHz`) | `2304,0 MHz` (`2304,0 MHz`) | `NONE` (`0`) | `NaN` |
| `#56` | `12:22:48` | Carga `~30 s` | `34,1 °C` | `1804,8 MHz` (`1804,8 MHz`) | `2169,6 MHz` (`2304,0 MHz`) | `NONE` (`0`) | `NaN` |
| `#63` | `12:23:23` | Carga `~66 s` (queda pontual) | `34,1 °C` | `1804,8 MHz` (`1804,8 MHz`) | `979,2 MHz` (`2304,0 MHz`) | `NONE` (`0`) | `NaN` |
| `#68` | `12:23:48` | Carga `~90 s` | `34,2 °C` | `1804,8 MHz` (`1804,8 MHz`) | `1708,8 MHz` (`2304,0 MHz`) | `NONE` (`0`) | `NaN` |
| `#74` | `12:24:18` | Carga `~120 s` | `34,2 °C` | `1804,8 MHz` (`1804,8 MHz`) | `2169,6 MHz` (`2304,0 MHz`) | `NONE` (`0`) | `NaN` |
| `#80` | `12:24:48` | Carga `~150 s` | `34,3 °C` | `1804,8 MHz` (`1804,8 MHz`) | `2169,6 MHz` (`2304,0 MHz`) | `NONE` (`0`) | `NaN` |
| `#84` | `12:25:09` | Fim da carga `~172 s` | `34,3 °C` | `1804,8 MHz` (`1804,8 MHz`) | `1843,2 MHz` (`2304,0 MHz`) | `NONE` (`0`) | `NaN` |
| `#85` | `12:25:14` | Pós-carga `+5 s` | `34,3 °C` | `768,0 MHz` (`1804,8 MHz`) | `979,2 MHz` (`2304,0 MHz`) | `NONE` (`0`) | `NaN` |

### Análise sob carga
A `policy0` (núcleos 0 a 5) permaneceu travada em `1804,8 MHz` (100%) em todas as 35 amostras da carga. A `policy6` (núcleos 6 e 7) operou predominantemente em `2169,6 MHz` (94,2%, mediana), com oscilações pontuais isoladas como na amostra `#63` (`979,2 MHz`), sem redução sustentada de `scaling_max_freq` (teto mantido em 100%) e sem mudança no `ThermalStatus` (`NONE`).

A carga de 3 minutos apenas de CPU elevou a bateria em `+0,2 °C`, o que não foi suficiente para acionar thermal throttling severo neste aparelho. Ainda assim, o ensaio foi suficiente para comprovar que as leituras de `cpufreq` e `PowerManager.getCurrentThermalStatus()` funcionam de forma contínua dentro do app e que a frequência responde imediatamente ao início e ao encerramento da carga.

## 4. Trecho mínimo de código de referência para D2

O trecho abaixo resume a lógica validada no arquivo `LeitorSinais.kt` do aplicativo descartável para leitura de `/sys/devices/system/cpu/cpufreq/policyN` e de `PowerManager.getCurrentThermalStatus()`, servindo como referência direta para a implementação da etapa D2.

```kotlin
import android.content.Context
import android.os.Build
import android.os.PowerManager
import java.io.File
import java.io.IOException

data class LeituraPolicy(
    val identificador: String,
    val scalingCurKhz: Long?,
    val scalingMaxKhz: Long?,
    val cpuinfoMaxKhz: Long?,
)

fun lerKhzSysfs(caminho: String): Long? {
    return try {
        File(caminho).readText().trim().toLongOrNull()
    } catch (_: SecurityException) {
        null
    } catch (_: IOException) {
        null
    }
}

fun lerPoliciesCpu(): List<LeituraPolicy> {
    val dirCpufreq = File("/sys/devices/system/cpu/cpufreq")
    val regexPolicy = Regex("^policy(\\d+)$")
    val pastas = dirCpufreq.listFiles()
        ?.filter { it.isDirectory && regexPolicy.matches(it.name) }
        ?.sortedBy { regexPolicy.matchEntire(it.name)?.groupValues?.get(1)?.toIntOrNull() ?: Int.MAX_VALUE }
        ?: return emptyList()

    return pastas.map { pasta ->
        LeituraPolicy(
            identificador = pasta.name,
            scalingCurKhz = lerKhzSysfs("${pasta.path}/scaling_cur_freq"),
            scalingMaxKhz = lerKhzSysfs("${pasta.path}/scaling_max_freq"),
            cpuinfoMaxKhz = lerKhzSysfs("${pasta.path}/cpuinfo_max_freq"),
        )
    }
}

fun lerStatusTermico(contexto: Context): Int? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        return null
    }
    val powerManager = contexto.getSystemService(PowerManager::class.java) ?: return null
    return try {
        powerManager.currentThermalStatus
    } catch (_: Exception) {
        null
    }
}
```

## 5. Decisão go ou no-go por sinal e Plano B

### Decisão por sinal

| Sinal | Decisão | Justificativa e ressalvas |
|---|---|---|
| `scaling_cur_freq` | **GO** | Leitura confirmada sem root e responsiva à carga. Ressalva operacional para outros aparelhos: tratar `IOException` e `SecurityException` por SELinux e preferir leitura por `policyN` para reduzir I/O e evitar defasagem entre núcleos do mesmo bloco. |
| `scaling_max_freq` (teto imposto) | **GO** | Leitura confirmada sem root, sem divergência entre núcleos da mesma policy. Manter a mesma ressalva de fallback caso outro fabricante bloqueie `sysfs`. |
| `cpuinfo_max_freq` (máximo de fábrica) | **GO** | Leitura confirmada sem root, útil como denominador fixo para detectar queda de teto. Manter a mesma ressalva de fallback caso outro fabricante bloqueie `sysfs`. |
| `PowerManager.getCurrentThermalStatus()` | **GO com ressalva** | Disponível e funcional no Android 12 (API 31). Exige proteção por `Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q` (API 29 / Android 10+), retornando `null` ou indisponível em versões anteriores. |
| `PowerManager.getThermalHeadroom(0)` | **NO-GO** | Retornou `NaN` em todas as 86 amostras no Moto G60. Como já era um sinal opcional, deve ficar fora do classificador principal. |
| Temperatura da bateria | **GO** | Disponível em todas as amostras via `ACTION_BATTERY_CHANGED` e já integrada na linha de base do projeto. |

### Decisão consolidada
**GO com ressalvas** para avançar para a implementação de D2 combinando leitura de CPU por `cpufreq/policyN` (`scaling_cur_freq`, `scaling_max_freq` e `cpuinfo_max_freq`), `PowerManager.getCurrentThermalStatus()` (API 29+) e temperatura da bateria, deixando `PowerManager.getThermalHeadroom(0)` fora do classificador principal.

### Plano B (fallback para bloqueio de `sysfs`)
Em outros fabricantes ou versões mais restritivas do Android, políticas de SELinux podem bloquear a listagem ou a leitura dos arquivos em `/sys/devices/system/cpu/cpufreq/`. Nesses casos, o adaptador Android deve capturar `IOException` e `SecurityException` e retornar frequência nula sem interromper a coleta nem lançar exceção para a interface. Com a frequência indisponível, o classificador continua operando em modo degradado usando temperatura da bateria, taxa de variação térmica e `PowerManager.getCurrentThermalStatus()`.

## 6. Limitações conhecidas desta validação

- **Amostra de aparelho único:** o teste foi executado em 1 aparelho (Motorola Moto G60, Android 12 / API 31). O acesso livre ao `sysfs` confirmado neste modelo não garante o mesmo comportamento em outros fabricantes ou em versões mais recentes do Android.
- **Carga insuficiente para throttling severo:** 3 minutos de carga somente de CPU aqueceram a bateria em `+0,2 °C`, sem provocar redução sustentada de `scaling_max_freq` nem saída de `ThermalStatus = NONE`. O comportamento desses dois sinais em cenários de aquecimento intenso (como carga prolongada de CPU e GPU ou jogos pesados) ainda precisará ser observado em campo.
