# Uso de Inteligencia Artificial

Durante la prueba (del 27 al 29 de octubre de 2026) usé la IA como **par de programación y revisora**. Trabajé con un enfoque ligero guiado por especificaciones: antes de cada historia fijé qué debía cumplirse y cómo lo iba a comprobar, y la IA trabajó dentro de esos límites.

Monté el proyecto, integré y ajusté el código en mi IDE, ejecuté cada comando y prueba, y tomé las decisiones de producto, alcance y arquitectura. La IA no tuvo acceso directo al repositorio.

## Índice

1. [Herramientas utilizadas](#1-herramientas-utilizadas)
2. [Forma de trabajo](#2-forma-de-trabajo)
3. [Constitución del proyecto](#3-constitución-del-proyecto)
4. [Especificación de cada historia](#4-especificación-de-cada-historia)
5. [Reparto del trabajo entre la IA y yo](#5-reparto-del-trabajo-entre-la-ia-y-yo)
6. [Prompts clave](#6-prompts-clave)
7. [Validación humana](#7-validación-humana)
8. [Lecciones](#8-lecciones)

## 1. Herramientas utilizadas

| Herramienta | Uso |
| --- | --- |
| **Claude (Anthropic)**, mediante Claude Code en una sesión en la nube | Análisis del enunciado, comparación de alternativas, propuesta de código y tests, diagnóstico de errores a partir de logs, planes de ejecución y capturas, y apoyo en la documentación |

Herramientas no basadas en IA con las que validé su trabajo: IntelliJ IDEA, Maven y npm, Testcontainers, GitHub Actions, `EXPLAIN ANALYZE`, k6, Bruno y las DevTools del navegador.

## 2. Forma de trabajo

Cada historia siguió los mismos pasos, sin más ceremonia que la necesaria para una prueba de pocos días:

1. **Especificar:** reescribir la historia del enunciado con sus criterios de aceptación (sección 4) y revisarla contra la constitución (sección 3).
2. **Planificar y dividir:** acordar el diseño y partir el trabajo en una rama con varias partes pequeñas, cada una compilable y probada.
3. **Implementar y verificar:** integrar el código, ejecutar los tests en local y abrir un PR; solo se fusionó con el CI en verde.

No usé una herramienta específica de SDD ni archivos de especificación separados: la especificación vivió en el enunciado, en los criterios de la sección 4 y en [`architecture.md`](architecture.md).

## 3. Constitución del proyecto

Estas reglas, derivadas del enunciado, fueron innegociables durante todo el desarrollo. Cada propuesta de la IA se aceptaba o se rechazaba contra ellas.

| # | Regla | Cómo se garantiza |
| --- | --- | --- |
| 1 | Prohibido `LIKE`, `ILIKE` o expresiones regulares para buscar; solo el motor de búsqueda indexado | PostgreSQL FTS con índice GIN y un test que falla si aparece `LIKE` en el código de búsqueda |
| 2 | Prohibido el *polling* para el estado de los documentos; solo SSE o WebSocket | SSE con heartbeat; el frontend no consulta el estado periódicamente |
| 3 | La subida responde de inmediato con el ID y el estado `PROCESANDO`; el procesamiento es asíncrono | `202 Accepted` y procesamiento en RabbitMQ con reintentos y DLQ |
| 4 | Búsqueda con p95 ≤ 1000 ms medida con 10.000 documentos o más, con evidencia en `docs/` | k6 con 100.007 documentos: p95 de 373 ms; resultados y planes de ejecución en `architecture.md` |
| 5 | Errores centralizados, validación en el borde y configuración por `.env` (con `.env.example`) | `GlobalExceptionHandler` con `ProblemDetail` (RFC 7807), validación en los controladores, `.env.example` versionado |
| 6 | Tests de unidad e integración para ingesta, indexación y búsqueda | Tests unitarios y de integración con Testcontainers (PostgreSQL y RabbitMQ reales), ejecutados en el CI |
| 7 | Cada fase deja notas para `docs/architecture.md` y `docs/ia.md` (prompts y correcciones manuales) | Al cerrar cada rama actualicé mis notas de decisiones y de uso de la IA, que dieron origen a estos dos documentos |

## 4. Especificación de cada historia

Cada historia partió de una especificación como estas, redactada a partir del enunciado.

**HU-01 Carga.** Como usuario quiero subir documentos técnicos en PDF, TXT o Markdown, uno a uno o varios a la vez, con título, autor, categoría, etiquetas y versión, para que queden disponibles para buscar. Se validan formato y tamaño. La subida responde de inmediato con el ID y el estado `PROCESANDO`; la extracción del texto y la indexación son asíncronas y terminan en `INDEXADO` o `ERROR`, con reintentos ante fallos transitorios.

**HU-02 Búsqueda.** Como usuario quiero buscar documentos técnicos por palabras clave o frases en título, metadatos y contenido para localizar rápido la documentación. Solo se buscan documentos ya indexados. Resultados paginados, ordenados por relevancia (el título pesa más que el contenido), con fragmentos coincidentes resaltados y un estado vacío claro. Debe responder en 1 s o menos con al menos 10.000 documentos.

**HU-03 Visor.** Como usuario quiero ver un documento en una pantalla dedicada, con su contenido estructurado y todos sus metadatos, para leerlo sin descargar el archivo. El Markdown se muestra renderizado y el texto de PDF y TXT se muestra legible. Los documentos muy grandes no deben bloquear el navegador.

**HU-04 Tiempo real.** Como usuario quiero recibir un aviso cuando mi documento pase a `INDEXADO` o `ERROR`, sin recargar la página, para saber cuándo puedo buscarlo. Sin *polling*. Si la conexión se corta, la interfaz se reconecta y recupera los cambios que ocurrieron mientras estaba desconectada.

## 5. Reparto del trabajo entre la IA y yo

| Área | Lo que hice yo | Lo que aportó la IA |
| --- | --- | --- |
| Proyecto y repositorio | Creé el repositorio y el monorepo; generé el backend con Spring Initializr y el frontend con Angular CLI; definí el flujo con una rama y un PR por bloque | Estructura de carpetas, workflow de GitHub Actions y convención de commits |
| Backend | Escribí los puertos de la arquitectura hexagonal (`application/port/in` y `application/port/out`), los modelos de dominio (`domain/model`) y los servicios de aplicación (`application/service`) antes de pedir la propuesta de la IA, y comparé ambas versiones; integré y ajusté el código en IntelliJ; configuré el `pom.xml`; corregí la estructura de carpetas y los archivos que faltaban en el control de versiones | Una versión de los puertos, modelos y servicios para contrastar con la mía, propuestas del resto del código y de los tests de cada parte, y diagnóstico de los fallos a partir de las trazas |
| Búsqueda y rendimiento | Generé los 100.000 documentos de prueba, ejecuté `EXPLAIN ANALYZE` y k6, y llevé los resultados a la mesa | Diseño de la consulta, scripts de seed y benchmark, e interpretación de los planes de ejecución |
| Frontend | Configuré Angular Material y el tema cyan en `styles.scss`; probé la interfaz y reporté con capturas lo que no se veía bien; decidí la interfaz en inglés | Servicios, componentes y tests |
| Docker | Levanté el entorno completo y corregí el `ENTRYPOINT` desde la terminal | Dockerfiles, configuración de nginx y `docker-compose.yml` |
| Documentación | Mis notas de cada rama, revisión y ajuste final del texto | Redacción inicial y diagramas |

## 6. Prompts clave

Durante el desarrollo trabajé con la IA de forma conversacional, historia por historia, a partir de las especificaciones de la sección 4. Para la revisión final del código preparé tres prompts estructurados por rol. Cada uno define el contexto, un proceso por fases con puntos de parada y un formato de salida verificable.

| Prompt | Rol que asume la IA | Alcance | Estado |
| --- | --- | --- | --- |
| [Revisión del backend](#prompt-de-revisión-del-backend) | Staff Engineer Java | `backend/` | Fases 0 y 1 ejecutadas el 29 de octubre de 2026 |
| [Revisión del frontend](#prompt-de-revisión-del-frontend) | Staff Frontend Engineer Angular | `frontend/` | Fases 0 y 1 ejecutadas el 29 de octubre de 2026 |
| [Estrategia de pruebas](#prompt-de-estrategia-de-pruebas) | SDET / Staff Engineer | `backend/` y `frontend/` | Fases 0 y 1 ejecutadas el 29 de octubre de 2026 |

Los tres comparten las mismas reglas: la IA no modifica nada hasta que yo apruebe los hallazgos, cada hallazgo cita archivo y línea, lo que no pudo verificar lo marca como `PLAUSIBLE` o `SUPUESTO`, y cada corrección va en un commit separado con Conventional Commits.

### Resultados de las revisiones

La fase 0 de cada prompt fija una línea base sin modificar el código; la fase 1 produce un informe de hallazgos y se detiene hasta que yo apruebe qué corregir.

**Backend (Staff Engineer Java)**

- **Fase 0, línea base:** `mvn verify` con Testcontainers: 58 tests, 0 fallos, `BUILD SUCCESS`.
- **Fase 1, revisión:** veredicto **"listo con reservas"**, con 17 hallazgos: ninguno crítico ni alto, 7 medios y 10 bajos. Los más relevantes:
    - El número de página no tiene límite: `page × size` puede desbordar el `int` y la búsqueda devuelve 500 en lugar de 400.
    - En la carga masiva, un fallo de infraestructura en un archivo tumba todo el lote.
    - El detalle del documento devuelve el contenido completo aunque el visor solo muestra 100.000 caracteres.
- **Fase 2, corrección:** organizada en lotes pequeños (primero los errores 500 provocables por el cliente, luego limpieza, configuración y tests). Las decisiones de producto que plantea (truncar o rechazar documentos de más de 5 millones de caracteres) quedan a mi criterio.

**Frontend (Staff Frontend Engineer Angular), fase 0**

| Comprobación | Resultado |
| --- | --- |
| Versiones | Angular 22.2, TypeScript 6.0, RxJS 7.8, Angular Material 22, Vitest 5 con jsdom |
| Modo estricto | `strict` activo por defecto en TypeScript 6 y `strictTemplates` activo por defecto en Angular (verificados, aunque no aparecen en `tsconfig.json`) |
| `ng build` (producción) | Correcto. Inicial: 535 kB (132 kB transferidos); aviso del presupuesto de 500 kB. Las tres páginas se cargan en diferido |
| `ng test` | 30 tests en 8 archivos, todos en verde |
| `ng lint` | No hay ESLint configurado en el proyecto |
| `npm audit` | Dependencias de producción: 0 vulnerabilidades. Desarrollo: 1 alta en el Vite que usa `@angular/build` (servidor de desarrollo) y 1 baja |

Arquitectura entendida: componentes standalone con OnPush y signals, rutas con carga diferida, una capa de API aislada (`DocumentApiService`), un interceptor de errores, el estado de búsqueda en la URL con `debounceTime` y `switchMap`, y un único `EventSource` compartido con reconexión y reconciliación del estado (`DocumentEventsService` y `UploadTracker`).

**Frontend, fase 1:** veredicto **"listo con reservas"**, con 15 hallazgos: ninguno crítico ni alto, 5 medios y 10 bajos. Los más relevantes:

- `provideMarkdown()` se registra en la raíz, así que `marked` y `ngx-markdown` (73 kB) entran en el bundle inicial aunque solo los usa el visor. Moverlo a la ruta del visor deja el bundle inicial por debajo del presupuesto de 500 kB.
- Al reconectar, el seguimiento de cargas descarga el documento completo de cada carga pendiente solo para leer su estado.
- Una página fuera de rango (por ejemplo, un enlace compartido) muestra "No documents match" aunque haya resultados, y oculta el paginador.
- No hay ESLint ni cabeceras de seguridad en nginx (CSP, `X-Content-Type-Options`, `Referrer-Policy`).

**Estrategia de pruebas (SDET), fase 0**

| Lado | Frameworks y utilidades | Tests | Cobertura base |
| --- | --- | --- | --- |
| Backend | JUnit 5, Mockito, AssertJ, Spring Boot Test, Testcontainers (PostgreSQL 17 y RabbitMQ 4 reales), Awaitility | 58 | 90,3 % de líneas y 78,9 % de ramas (JaCoCo) |
| Frontend | Vitest 5 con jsdom, TestBed, `HttpTestingController`, un `EventSource` falso para el tiempo real | 30 | 57,3 % de sentencias y 57,0 % de ramas en todo `src/app` |

La cobertura del frontend que muestra Vitest por defecto (82,6 %) solo cuenta los archivos que algún test carga. Midiendo todo `src/app`, aparecen los archivos sin tests.

Clasificación por riesgo de lo que queda sin cubrir:

| Riesgo | Código | Cobertura actual |
| --- | --- | --- |
| Alto | Página de carga (`upload-page.ts`) y su lista de cargas recientes | 0 % |
| Alto | Capa de API del frontend (`document-api.service.ts`) e interceptor de errores | 11 % y 0 % |
| Alto | Almacenamiento local y su protección contra rutas fuera del directorio (`LocalFileStorageAdapter`) | 52 % de líneas |
| Medio | Paso de la DLQ al estado `ERROR` (`DocumentProcessingListener`) | 50 % de líneas |
| Medio | Detección del tipo real del archivo (`TikaContentTypeDetector`) | 67 % de líneas |
| Bajo | Dominio, búsqueda FTS, persistencia y notificaciones del backend | Entre 98 % y 100 % de líneas |

**Pruebas, fase 1:** una matriz de 16 grupos de pruebas, priorizada por riesgo. Siguiendo la pirámide, son sobre todo unitarias, con integración solo en las fronteras reales y sin e2e nuevos (la prueba de humo del CI ya recorre la pila completa).

| Prioridad | Qué se prueba | Tipo |
| --- | --- | --- |
| Alta | Página de carga: modo individual y por lotes, título sugerido, errores por campo, 413 y resultado del lote | Componente con `HttpTestingController` |
| Alta | Capa de API del frontend e interceptor de errores | Unitarias con `HttpTestingController` |
| Alta | Almacenamiento local: guardar, abrir, borrar y rechazo de claves con `../` | Unitaria con directorio temporal |
| Alta | Desbordamiento de página y fallos parciales del lote, con el test que falla primero | Unitaria y `@WebMvcTest` |
| Media | Paso por la DLQ a `ERROR`, idempotencia con mensajes duplicados concurrentes y cliente SSE lento | Integración con Testcontainers |
| Media | Manejo de errores HTTP (503 del broker, 500 sin detalles internos), detección del tipo real del archivo y límite de caracteres extraídos | `@WebMvcTest` y unitarias parametrizadas |
| Media | Búsqueda en el frontend: página fuera de rango, error 400 y aviso de documentos nuevos | Componente |
| Baja | Lista de cargas recientes, eventos SSE malformados y formato de tamaños | Componente y unitarias |

Para medir la cobertura de forma permanente propone añadir JaCoCo al `pom.xml` y `@vitest/coverage-v8` al frontend. Son herramientas de prueba, no dependencias de producción, y quedan pendientes de mi aprobación igual que la implementación de la matriz.

### Prompt de revisión del backend

<details>
<summary>Ver el prompt completo</summary>

```text
<rol>
Actúa como Staff Engineer Java con experiencia en Spring Boot, diseño de servicios y revisión de código para producción. Eres riguroso y pragmático: priorizas por impacto real, no por preferencia personal.
</rol>

<contexto>
- Alcance: backend/src/main/java/com/kata/docsearch
- Stack: Java 21, Spring Boot 4.1, build Maven, BD PostgreSQL 17, cola RabbitMQ 4
- Arquitectura declarada: Hexagonal
- Restricciones del proyecto: prohibido LIKE para búsquedas; prohibido polling; sin dependencias nuevas sin justificar
- Objetivo: prueba técnica
</contexto>

<proceso>
FASE 0 - Reconocimiento (no modifiques nada)
1. Lee el archivo de build, la estructura de paquetes, la configuración (application*.yml/properties) y los tests existentes.
2. Ejecuta compilación y tests para fijar una línea base (`mvn verify` o `./gradlew check`). Si no puedes ejecutarlos, dilo y continúa solo con análisis estático.
3. Resume en 5-8 líneas cómo entiendes la arquitectura y el flujo principal. Marca como SUPUESTO todo lo que no hayas podido verificar.

FASE 1 - Revisión (solo lectura, NO edites archivos)
Evalúa el alcance en estas dimensiones:
1. Corrección: NPE y mal uso de Optional, equals/hashCode, estado mutable compartido, condiciones de carrera, recursos sin cerrar, fechas y zonas horarias, dinero con double.
2. Diseño: controladores delgados, lógica en servicios/dominio, entidades JPA no expuestas en la API (usar DTO/records), inyección por constructor, SOLID, acoplamiento, dependencias cíclicas, límites entre capas.
3. API y errores: semántica REST y códigos HTTP, Bean Validation en el borde, @RestControllerAdvice con ProblemDetail, excepciones tipadas, sin catch vacíos ni la misma excepción logueada varias veces.
4. Persistencia y rendimiento: N+1, LAZY/EAGER, paginación, proyecciones, índices, límites de transacción (readOnly, auto-invocación que anula @Transactional), llamadas bloqueantes dentro de transacciones, batch.
5. Seguridad (OWASP): inyección, autorización por recurso, validación de archivos subidos (tipo real, tamaño, path traversal), secretos en código o logs, datos personales en logs, CORS, dependencias con CVE conocidos.
6. Concurrencia y asincronía: @Async con pools configurados, idempotencia y reintentos, thread-safety de beans singleton, manejo de eventos y colas.
7. Observabilidad: logs estructurados con niveles correctos y correlation id, métricas, health checks.
8. Calidad de código: nombres, métodos largos, complejidad ciclomática, duplicación, código muerto, números mágicos, uso adecuado de Java moderno (records, switch expressions, streams sin abusar), uso consistente de Lombok.
9. Configuración: externalizada por perfiles, @ConfigurationProperties validadas, nada hardcodeado, `.env.example` limpio.
10. Testabilidad: dependencias estáticas, falta de seams, rutas críticas sin tests.

Entrega el informe y DETENTE. No pases a la Fase 2 hasta que yo apruebe qué hallazgos corregir.

FASE 2 - Refactorización (solo hallazgos aprobados)
- Un hallazgo (o grupo coherente) por cambio, con mensaje en Conventional Commits.
- Preserva el comportamiento observable. Si el hallazgo es un bug: primero un test que falle, luego la corrección, en un cambio separado.
- Si el código a tocar no tiene tests, escribe antes tests de caracterización.
- Tras cada cambio ejecuta compilación y tests. Si algo falla, revierte ese cambio, repórtalo y continúa con el siguiente.
- No cambies contratos públicos (endpoints, DTOs, esquema de BD) sin marcarlos como BREAKING y esperar mi confirmación. Los cambios de esquema van con Flyway/Liquibase, nunca con ddl-auto.
- No agregues dependencias sin justificarlas, no reformatees archivos completos y no toques código fuera del alcance.
- Respeta el estilo existente salvo que el estilo sea el problema.
</proceso>

<formato_de_salida_fase_1>
A. Resumen ejecutivo (máx. 10 líneas): veredicto (listo / listo con reservas / no listo), 3 riesgos principales y 2-3 puntos fuertes.
B. Tabla de hallazgos ordenada por severidad, con columnas: ID | Severidad | Dimensión | Ubicación (archivo:línea) | Problema | Impacto | Recomendación | Esfuerzo (S/M/L) | Riesgo de regresión.
   Severidad: Crítico = fallo, pérdida de datos o vulnerabilidad explotable; Alto = degradación seria o bug probable; Medio = mantenibilidad o rendimiento mejorable; Bajo = estilo y pulido.
C. Plan de refactorización en lotes pequeños, con el orden y las dependencias entre cambios.
D. Supuestos y preguntas abiertas.
Reglas: cada hallazgo cita archivo y línea. No inventes problemas. Si no estás seguro, márcalo PLAUSIBLE y di cómo verificarlo. Si un patrón se repite, agrúpalo en un solo hallazgo con la lista de ubicaciones.
</formato_de_salida_fase_1>

<cierre_fase_2>
Entrega una tabla antes/después (hallazgos resueltos y pendientes, tests agregados, resultado del build), los riesgos residuales y los siguientes pasos recomendados.
</cierre_fase_2>
```

</details>

### Prompt de revisión del frontend

<details>
<summary>Ver el prompt completo</summary>

```text
<rol>
Actúa como Staff Frontend Engineer especialista en Angular, TypeScript y RxJS, con foco en rendimiento, accesibilidad y mantenibilidad. Priorizas por impacto real para el usuario y para el equipo.
</rol>

<contexto>
- Alcance: frontend/src/app
- Stack: Angular 22, TypeScript, RxJS + signals, UI Angular Material, tests Vitest
- Restricciones del proyecto: el estado de los documentos llega por SSE, sin polling
- Objetivo: prueba técnica
- Detecta la versión real de Angular y adapta las recomendaciones: si es antigua, propón una migración gradual con los schematics oficiales (control flow, standalone, inputs/outputs con signals) cuando existan, no reescrituras masivas.
</contexto>

<proceso>
FASE 0 - Reconocimiento (no modifiques nada)
1. Lee package.json, angular.json, tsconfig (¿strict?), la configuración de ESLint y la estructura de carpetas.
2. Ejecuta lint, tests y build de producción (`ng lint`, `ng test --watch=false`, `ng build`) para fijar una línea base, incluido el tamaño de bundle.
3. Resume en 5-8 líneas la arquitectura y el flujo de datos. Marca como SUPUESTO lo que no verificaste.

FASE 1 - Revisión (solo lectura, NO edites archivos)
Evalúa el alcance en estas dimensiones:
1. Arquitectura: estructura por features, componentes contenedores vs presentacionales, standalone, rutas con lazy loading, servicios con una sola responsabilidad, sin lógica de negocio en plantillas.
2. Reactividad y estado: signals (signal, computed, input/output/model, effect con moderación) y RxJS bien usados; sin subscribe anidados; switchMap/concatMap/exhaustMap según el caso; async pipe o toSignal; takeUntilDestroyed; sin fugas de suscripciones; estado sin mutación; NgRx o SignalStore solo si se justifica.
3. Rendimiento: ChangeDetectionStrategy.OnPush, `track` en @for (o trackBy), @defer, NgOptimizedImage, virtual scroll en listas grandes, sin funciones ni getters costosos en la plantilla, debounce en búsquedas, cancelación de peticiones obsoletas, tamaño de bundle.
4. TypeScript: `strict` activo, sin `any`, tipos de dominio y DTOs separados, formularios reactivos tipados, uniones literales en lugar de strings sueltos.
5. HTTP y errores: capa de API aislada, interceptors (auth, errores, loading), ErrorHandler global, estados loading/vacío/error en cada vista. Tiempo real (SSE/WebSocket): cierre en destroy, reconexión con backoff y recuperación del estado actual al reconectar.
6. Seguridad: sin innerHTML ni bypassSecurityTrust* sin sanitizar, almacenamiento de tokens, CSP, sin secretos en environment, validación del cliente solo como UX.
7. Accesibilidad (WCAG 2.2 AA): HTML semántico, labels, foco y teclado, ARIA solo cuando haga falta, contraste, aria-live para notificaciones en tiempo real.
8. UX/UI: consistencia, responsive, feedback de carga y error, validación de formularios clara, estados vacíos.
9. Calidad: nombres, tamaño de componentes, duplicación, plantillas legibles (@if/@for/@switch), estilos encapsulados, angular-eslint y Prettier, `npm audit`, i18n.
10. Testabilidad: uso de inject(), selectores estables (roles, data-testid), componentes sin dependencias ocultas.

Entrega el informe y DETENTE. No pases a la Fase 2 hasta que yo apruebe qué hallazgos corregir.

FASE 2 - Refactorización (solo hallazgos aprobados)
- Un hallazgo (o grupo coherente) por cambio, con mensaje en Conventional Commits.
- Preserva el comportamiento visible para el usuario. Si es un bug: primero un test que falle, luego la corrección, en un cambio separado.
- Si el código a tocar no tiene tests, escribe antes tests de caracterización.
- Tras cada cambio ejecuta lint, tests y `ng build`. Si algo falla, revierte ese cambio, repórtalo y continúa.
- No cambies contratos con el backend ni rutas públicas sin marcarlos como BREAKING y esperar mi confirmación.
- No agregues dependencias sin justificarlas, no reformatees archivos completos y no toques código fuera del alcance.
</proceso>

<formato_de_salida_fase_1>
A. Resumen ejecutivo (máx. 10 líneas): veredicto (listo / listo con reservas / no listo), 3 riesgos principales y 2-3 puntos fuertes.
B. Tabla de hallazgos ordenada por severidad: ID | Severidad | Dimensión | Ubicación (archivo:línea) | Problema | Impacto | Recomendación | Esfuerzo (S/M/L) | Riesgo de regresión.
   Severidad: Crítico = fallo funcional, fuga de memoria seria o vulnerabilidad; Alto = degradación notable o bug probable; Medio = mantenibilidad o rendimiento mejorable; Bajo = estilo y pulido.
C. Plan de refactorización en lotes pequeños, con orden y dependencias.
D. Supuestos y preguntas abiertas.
Reglas: cada hallazgo cita archivo y línea. No inventes problemas. Si no estás seguro, márcalo PLAUSIBLE y di cómo verificarlo. Agrupa los patrones repetidos en un solo hallazgo con la lista de ubicaciones.
</formato_de_salida_fase_1>

<cierre_fase_2>
Entrega una tabla antes/después (hallazgos resueltos y pendientes, tests agregados, tamaño de bundle, resultado de lint/test/build), los riesgos residuales y los siguientes pasos recomendados.
</cierre_fase_2>
```

</details>

### Prompt de estrategia de pruebas

<details>
<summary>Ver el prompt completo</summary>

```text
<rol>
Actúa como SDET/Staff Engineer con dominio de pruebas en Java (Spring Boot) y Angular. Escribes tests que detectan regresiones reales, no tests que solo inflan la cobertura.
</rol>

<contexto>
- Alcance: ambos, en backend/src y frontend/src/app
- Meta: cubrir las rutas críticas (ingesta, indexación, búsqueda y tiempo real) sin bajar la cobertura actual
- Restricciones: no modificar código de producción salvo lo mínimo para testabilidad
- Usa los frameworks de pruebas que el proyecto YA tiene. Si no hay ninguno, propón uno y espera mi confirmación.
</contexto>

<proceso>
FASE 0 - Reconocimiento (no escribas tests todavía)
1. Detecta frameworks, convenciones de nombres y utilidades de test existentes.
2. Ejecuta la suite y mide la cobertura base (`mvn verify` o `./gradlew test jacocoTestReport`; `ng test --watch=false --code-coverage`).
3. Clasifica el código por riesgo: lógica de negocio, validaciones, manejo de errores, seguridad, concurrencia/asincronía/tiempo real, integraciones.

FASE 1 - Plan de pruebas
Presenta una matriz: Unidad | Tipo (unitaria / slice / integración / e2e) | Escenarios (camino feliz, límites, nulos o vacíos, errores, permisos, concurrencia) | Dobles necesarios | Prioridad.
Respeta la pirámide: muchas unitarias, integración en las fronteras reales y muy pocos e2e. DETENTE y espera mi OK. Si respondo "autónomo", continúa con las prioridades altas.

FASE 2 - Implementación

Backend (Java):
- JUnit 5 + Mockito + AssertJ; usa tests parametrizados para variantes de datos.
- Controladores: @WebMvcTest para rutas, validación, códigos HTTP y cuerpo de error (ProblemDetail).
- Persistencia: @DataJpaTest con Testcontainers del MISMO motor que producción. No uses H2 si hay funciones específicas del motor (p. ej. full-text search de PostgreSQL).
- @SpringBootTest solo para flujos de integración críticos (p. ej. subir documento -> cola -> indexado -> evento emitido).
- Asincronía: Awaitility en lugar de Thread.sleep. HTTP externo: WireMock o MockWebServer. Tiempo: Clock inyectable.
- Datos de prueba con builders u Object Mother. Cubre transaccionalidad, idempotencia y autorización cuando aplique.

Frontend (Angular):
- Componentes: TestBed con harnesses de Angular CDK o Testing Library. Prueba lo que ve y hace el usuario, no detalles internos. Selectores por rol, texto o data-testid.
- Servicios HTTP: provideHttpClient + provideHttpClientTesting + HttpTestingController, con `verify()` al final.
- RxJS: TestScheduler (marbles) o fakeAsync/tick. Signals y computed: pruébalos a través del componente o servicio.
- Tiempo real: doble controlable de EventSource/WebSocket; cubre conexión, mensaje, error, reconexión y limpieza en destroy.
- Guards, interceptors, pipes y validators: pruebas unitarias puras.
- e2e (Playwright/Cypress) solo si el proyecto ya lo usa, y solo para 1-3 flujos críticos.

Reglas de calidad (ambos):
- Estructura Arrange-Act-Assert; nombre que describa escenario y resultado, siguiendo la convención del repo.
- Un comportamiento por test; sin if ni bucles dentro del test (usa parametrización).
- Deterministas y aislados: sin sleeps, sin orden implícito, sin red real, sin hora real, sin estado compartido.
- Mockea solo fronteras (BD, red, tiempo, azar). No mockees el sistema bajo prueba.
- Asserts significativos: nada de `assertNotNull` aislado ni de "no lanza excepción" como única verificación. Verifica interacciones solo cuando son el comportamiento.
- No pruebes código del framework ni getters/setters triviales.
- La cobertura es una señal, no la meta. Si hay mutation testing (PIT/Stryker), úsalo en los módulos críticos.
- No modifiques código de producción salvo cambios mínimos de testabilidad (inyectar Clock, extraer interfaz) y lístalos aparte.
- Si un test revela un bug, NO lo corrijas en silencio: deja el test marcado (deshabilitado o fallo esperado, con el motivo) y repórtalo.
- Valida los tests: ejecútalos varias veces para detectar flakiness y rompe a propósito 1-2 reglas de negocio para confirmar que los tests fallan.
</proceso>

<formato_de_salida>
1. Archivos creados o modificados.
2. Resultado de la ejecución (pasan / fallan / omitidos).
3. Cobertura antes/después (líneas y ramas) por módulo.
4. Bugs encontrados, con test que los evidencia.
5. Cambios de testabilidad en código de producción.
6. Huecos pendientes y siguiente paso recomendado.
</formato_de_salida>
```

</details>

## 7. Validación humana

### Criterios para aceptar una propuesta

- Cumple la constitución (sección 3) y los criterios de su historia (sección 4).
- Compila y pasa los tests en local **y** en el CI del pull request; nunca se fusionó con checks en rojo.
- Las versiones de librerías, acciones de GitHub e imágenes Docker se comprobaron en su registro oficial.
- Puedo explicar cada línea de configuración y de código.
- Los resultados se comprobaron en ejecución: curl, Bruno, `EXPLAIN`, k6, DevTools y capturas de pantalla.

### Errores de la IA detectados y corregidos

| Propuesta o suposición de la IA | Cómo se detectó | Corrección |
| --- | --- | --- |
| springdoc 3.0.3 con Spring Boot 4.1.2 | Revisión de compatibilidad en Maven Central | springdoc 3.2.0 |
| Toda `IOException` del parser de Tika tratada como fallo de disco | Falló un test unitario: un PDF corrupto se habría reintentado | `TaggedInputStream` distingue error de lectura (reintento) de error de contenido (`ERROR`) |
| `TaggedInputStream.get(...)`, API de Tika 2 | Error de compilación | API vigente de commons-io |
| Servicio de búsqueda registrado en una parte y su adaptador en la siguiente | Fallaron 7 tests: el contexto de Spring no arrancaba | Adaptador adelantado; SQL probado antes en un PostgreSQL real |
| Primer seed con un vocabulario de ~170 palabras | Al medir, cada palabra aparecía en el 100 % de los documentos | Seed realista, y además de más de 10 min a menos de 1 min |
| `COUNT(*)` separado para el total | `EXPLAIN ANALYZE`: el conteo costaba más que la propia página | `count(*) OVER ()` en la misma consulta |
| El ranking arrastraba el contenido de todas las coincidencias | Mi `EXPLAIN` mostró volcado a disco en la función de ventana | Ranking con columnas ligeras y memoria de Postgres ajustada: p95 de 762 a 373 ms |
| `command` de Docker Compose en un bloque YAML plegado | PostgreSQL no arrancó | `command` como lista YAML |
| `EventSource` usado en un componente probado con jsdom | Fallaron los tests: jsdom no implementa `EventSource` | Fábrica inyectable con un `EventSource` falso |
| `ngx-markdown` con su extensión opcional de KaTeX | El servidor de Vite no arrancaba | `externalDependencies` en `angular.json` |
| Colores fijos fuera del tema de Material | Mi revisión del `styles.scss` | Tokens `--mat-sys-*` del tema |
| Fragmento de CSS abreviado | Captura: la zona de arrastre había perdido su diseño | Bloque completo |
| Título automático solo si el campo estaba vacío | Captura: un documento con el título de otro archivo | Se sustituye el título sugerido si el usuario no lo editó |
| nginx escuchando solo en IPv4 | El contenedor del frontend quedó *unhealthy* | `listen [::]:80` y healthcheck contra `127.0.0.1` |
| Campo de tags sin `matChipInputAddOnBlur`: un tag escrito sin pulsar Enter se descartaba | Prueba manual: el documento se guardaba sin el tag que había escrito | Primero un test de regresión, que fallaba en 2 casos; después la corrección, con los 33 tests del frontend en verde |

### Correcciones manuales en el proceso

- **Diez archivos sin commit** (la migración `V3` y el modelo de búsqueda): en local todo pasaba, pero el CI no compilaba. Los añadí y desde entonces reviso `git status --short` antes de cada push.
- **Un `ENTRYPOINT` con las comillas alteradas** hizo que el backend se reiniciara con código 127. Lo corregí reescribiendo la línea desde la terminal.
- **Carpeta del proyecto anidada** al generar el repositorio: la aplané antes del primer commit.

### Decisiones tomadas por mí, no por la IA

- Java + Spring Boot y Angular, en lugar de la recomendación inicial (NestJS + React).
- Una rama y un PR por bloque, y un pipeline de CI desde el principio.
- Commits, código, API e interfaz en inglés; documentación de entrega en español.
- Cuestionar la necesidad del seed y del benchmark antes de aceptarlos.
- No traducir los errores de la API y pasar la interfaz a inglés.
- Mantener el tema cyan de Material 3, aceptando el tono que exige la accesibilidad.

## 8. Lecciones

- **Unas reglas claras desde el inicio ahorran discusiones.** Con la constitución y los criterios de cada historia, cada propuesta se aceptaba o rechazaba con un hecho.
- **Verificar contra la versión exacta, no contra la memoria.** Los errores de API aparecieron cuando la IA respondió de memoria.
- **La evidencia completa acorta el diagnóstico.** Un `EXPLAIN` o un log pegado tal cual llevó a la causa en un paso.
- **Los tests y el CI son la red de seguridad.** Varios errores se detectaron antes de llegar a `main`.
- **La IA propone y yo decido.** Las mejores decisiones del proyecto salieron de cuestionar sus propuestas.
