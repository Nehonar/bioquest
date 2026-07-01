# BioQuest

> Un RPG retro/sci-fi de salud fisica y mental centrado en widgets. Convierte
> habitos reales (agua, fruta, comida saludable, caprichos, ejercicio, mood,
> peso, pasos) en stats de RPG, quests y un sistema de **corrupcion por
> patrones**. Local-first: sin login, sin nube, sin IA.

```text
BIO CORE // STATUS: STABLE
--------------------------------
VITALITY      [#######---] 72
RECOVERY      [#####-----] 51
NUTRITION     [######----] 64
FOCUS         [####------] 43
CORRUPTION    [##--------] 18
--------------------------------
QUEST: DRINK WATER +250 ml
WARNING: FRUIT DEFICIT
```

BioQuest **no es una app medica ni de dieta**. Es una capa motivadora que ayuda
a tomar 2-3 mejores decisiones al dia. No usa la idea de "alimentos prohibidos":
usa **impacto acumulado** sobre una ventana movil de 30 dias.

---

## Estado del proyecto (Fase 1 – MVP compilable)

Esta primera entrega prioriza, como pide el plan de producto, una version
compilable con **Dashboard, Daily Log, Room, motor de stats/corrupcion y
widget 4x2**, mas el resto de pantallas del MVP.

Incluido y funcional:

- Motor de **stats transparente** (`CalculateStatsUseCase`) — cada stat guarda
  sus contribuciones y puede explicar por que subio o bajo.
- Motor de **corrupcion por ventana de 30 dias** (`CalculateCorruptionUseCase`)
  con acumulacion, cadena (chain damage) y mitigacion por contexto positivo.
- **Persistencia Room** (8 entidades) + **DataStore** para preferencias.
- **Widget Bio Core 4x2** (Glance) con 4 stats + corrupcion y dos filas de
  botones de accion rapida como "chips" con fondo de color: fila positiva
  (Agua / Fruta / Sano) y fila neutra+riesgo (Normal / Procesado / Bolleria).
  El layout es compacto a proposito: un Column de Glance no hace scroll, asi
  que los botones se mantienen siempre visibles y pulsables.
- **Widget Quick Action 1x1** configurable (Agua / Fruta / Mood / Capricho).
- Pantallas Compose: **Dashboard, Daily Log, Quest Board, History, Settings**.
- **Quests** diarias adaptativas + semanales.
- **Datos demo** (~3 semanas) sembrados al primer arranque: la app y el widget
  se ven sin configurar nada.
- **Health Connect** como capa opcional (pasos) + fallback `SensorManager`.
- **WorkManager** para sync periodica y **recordatorios contextuales**, con
  **cadencia configurable** (1-8 h) desde Settings. Si pasan las horas sin
  registrar nada en horario diurno, un **check-in de inactividad** pregunta
  "¿has bebido o comido?" con botones de accion (Agua / Sano / Bolleria) que
  registran el habito directamente desde la notificacion.
- **Tests unitarios** de los dos motores (18 tests, verdes).

Deuda tecnica / siguientes fases (documentada, fuera del alcance de Fase 1):

- El fallback de `SensorManager` solo detecta soporte y el contador acumulado;
  convertirlo en pasos/dia necesita persistir un baseline.
- La pantalla de configuracion del widget 1x1 (elegir accion por instancia) no
  tiene UI todavia; lee el valor de `WidgetActionConfig` con `WATER` por defecto.
- `DailyStatSnapshot` / `QuestCompletion` estan modelados y escritos por el
  worker pero aun no alimentan graficas de tendencia.
- Edicion de reglas de comida desde Settings es de solo lectura por ahora.

---

## Arquitectura

Arquitectura limpia en un unico modulo `app`, separada por capas:

```text
com.bioquest
├── domain            # PURO Kotlin, sin Android. Modelos + casos de uso.
│   ├── model
│   ├── usecase
│   ├── repository    # puertos (interfaces)
│   ├── GameSnapshot  # estado compuesto del dia
│   └── BioQuestEngine# orquesta los casos de uso contra el repositorio
├── data
│   ├── local         # Room: entidades, DAOs, DB, mappers
│   ├── repository    # implementacion del puerto de dominio
│   ├── health        # Health Connect + SensorManager (opcional)
│   └── demo          # sembrado de datos demo
├── ui                # Compose: theme, componentes, screens, navigation
├── widget            # Glance: Bio Core 4x2 + Quick Action 1x1
├── workers           # WorkManager: sync periodica
├── notifications     # recordatorios contextuales
├── settings          # DataStore
└── di                # AppContainer (DI manual, sin Hilt)
```

**Principio clave:** todo el calculo vive en `domain` como funciones puras y
testeables. `BioQuestEngine` es el unico punto que toca el repositorio; debajo
de el, cada `UseCase` es determinista y esta cubierto por tests.

### Casos de uso

`CalculateStatsUseCase`, `CalculateCorruptionUseCase`,
`GenerateDailyQuestsUseCase`, `LogHabitUseCase`, `DetectHealthEventsUseCase`,
`BuildWidgetStateUseCase`, `ExplainStatUseCase`.

### Entidades Room

`HabitLogEntity`, `FoodRuleEntity`, `UserGoalEntity`, `StepCountEntity`,
`DailyStatSnapshotEntity`, `QuestCompletionEntity`, `HealthEventEntity`,
`WidgetActionConfigEntity`.

---

## Sistema de corrupcion (el diferenciador)

Sin alimentos prohibidos. Cada comida de riesgo suma **puntos de corrupcion**
a una ventana movil de 30 dias.

| Evento | Nutrition XP | Corruption |
|---|---:|---:|
| Agua 250 ml | +1 Vitality XP | 0 |
| Fruta | +2 | 0 |
| Comida saludable | +3 | 0 |
| Comida normal | +1 | 0 |
| Helado | 0 | +2 |
| Croissant/bolleria | 0 | +3 |
| Fast food/pizza | 0 | +4 |
| Atracon ultraprocesado | 0 | +8 |

Bandas (30 dias, editables en Settings):

- `0–12` Normal · `13–24` Vigilancia · `25–40` Riesgo alto · `41+` Evento critico

Tres mecanismos:

1. **Acumulacion** — suma de puntos en 30 dias (define la banda).
2. **Cadena** — 3+ dias seguidos con comida de riesgo → *Chain Damage*.
3. **Contexto positivo** — agua/fruta/pasos reducen parte del impacto del dia,
   pero **nunca borran mas de la mitad** ("sin borrar lo ocurrido").

Ejemplos (cubiertos por tests):

```text
4 helados x 2                       = 8 pts  -> Normal
4 helados x 2 + 15 croissants x 3   = 53 pts -> Evento critico
```

---

## Build

Requisitos: JDK 17+, Android SDK Platform 36 (compileSdk 36, exigido por
Health Connect), Android Gradle Plugin 8.9.1+, un dispositivo/emulador
Android 8.0+ (minSdk 26). `targetSdk` se mantiene en 35.

```bash
# Compilar debug
./gradlew :app:assembleDebug

# Ejecutar tests unitarios del motor (JVM, no requiere emulador)
./gradlew :app:testDebugUnitTest
```

> Los tests del dominio son Kotlin/JVM puro. Se pueden ejecutar sin Android SDK
> copiando `app/src/main/kotlin/com/bioquest/domain` y `app/src/test/...` a un
> proyecto `kotlin("jvm")` con JUnit — asi se validaron los 16 tests de esta
> entrega.

Al primer arranque se siembran reglas de comida por defecto y ~3 semanas de
historial demo para poder ver stats, corrupcion, quests y widget sin configurar
Health Connect.

---

## Permisos

Todos opcionales; la app funciona **completa de forma manual sin ninguno**:

- `ACTIVITY_RECOGNITION` (Android 10+): fallback de pasos con `SensorManager`.
- `POST_NOTIFICATIONS` (Android 13+): recordatorios contextuales.
- `health.READ_STEPS`: import opcional de pasos desde Health Connect.

Se piden solo cuando aportan valor, explicando el beneficio.

---

## Fuera de alcance (por diseno)

Sin login, sin nube, sin IA, sin social/ranking, sin recetas ni escaner de
codigos de barras. El foco es el widget y el motor de patrones.

## Referencias tecnicas

- [Health Connect](https://developer.android.com/health-and-fitness/health-connect)
- [Jetpack Glance](https://developer.android.com/develop/ui/compose/glance)
- [WorkManager PeriodicWorkRequest](https://developer.android.com/reference/androidx/work/PeriodicWorkRequest)
- [Notification runtime permission](https://developer.android.com/develop/ui/compose/notifications/notification-permission)
- [Step counter con SensorManager](https://developer.android.com/health-and-fitness/fitness/basic-app/read-step-count-data)
