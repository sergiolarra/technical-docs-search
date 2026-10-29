# Technical Docs Search

Buscador y visor de documentos técnicos (PDF, TXT y Markdown) con indexación en segundo plano, búsqueda de texto completo y notificaciones en tiempo real.

| Historia | Qué incluye |
| --- | --- |
| HU-01 Carga | Carga individual y masiva con metadatos; validación de formato, tamaño y tipo real; respuesta inmediata `202` con estado `PROCESANDO` |
| HU-02 Búsqueda | Texto completo sobre título, metadatos y contenido **sin `LIKE`**: sin distinguir tildes ni mayúsculas, frases, exclusiones, filtros, paginación y resaltado. p95 de 373 ms con 100.000 documentos |
| HU-03 Visor | Vista dedicada con metadatos y contenido: Markdown renderizado o texto plano |
| HU-04 Tiempo real | Server-Sent Events: la interfaz se actualiza al pasar a `INDEXADO` o `ERROR`, sin polling |

**Stack:** Java 21 · Spring Boot 4 · PostgreSQL 17 (Full-Text Search) · RabbitMQ 4 · Apache Tika · Angular 22 · Angular Material 3 · Docker.

## Arranque rápido

Requisito: Docker (Docker Desktop, OrbStack o Docker Engine con Compose v2).

```bash
docker compose up --build
```

| Servicio | URL |
| --- | --- |
| Aplicación web | http://localhost |
| API (acceso directo) | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Estado de la API | http://localhost:8080/actuator/health |
| RabbitMQ | http://localhost:15672 (`docsearch` / `changeme`) |

No hace falta crear `.env`: todas las variables tienen valor por defecto. Para cambiarlas, copia `.env.example` a `.env`.

Los servicios arrancan en orden según sus healthchecks: PostgreSQL y RabbitMQ, después el backend y por último el frontend. El primer build tarda unos minutos porque descarga las dependencias de Maven y npm.

## Desarrollo local

Requisitos: Java 21, Node.js (versión en `.nvmrc`) y Docker.

```bash
cp .env.example .env
docker compose up -d postgres rabbitmq     # solo infraestructura
cd backend && ./mvnw spring-boot:run       # API en :8080
cd frontend && npm install && npm start    # app en :4200; redirige /api a :8080
```

Si la pila completa de Docker está en marcha, detén antes su backend: ambos usan el puerto 8080.

## Estructura del repositorio

```
├── backend/            API Spring Boot (domain, application, infrastructure)
├── frontend/           Aplicación Angular (core, features, shared) e imagen nginx
├── packages/shared/    Reservado para código compartido (no se usa en esta versión)
├── docs/
│   ├── architecture.md Arquitectura, decisiones, tiempo real, escalabilidad y rendimiento
│   └── ia.md           Uso de inteligencia artificial
├── bruno/              Colección de Bruno con aserciones y archivos de ejemplo
├── scripts/
│   ├── seed/           Datos sintéticos para el benchmark
│   └── benchmark/      EXPLAIN y prueba de carga con k6
├── .github/workflows/  CI: backend, frontend y pila completa en Docker
└── docker-compose.yml
```

## API

| Método | Ruta | Descripción |
| --- | --- | --- |
| `POST` | `/api/documents` | Carga individual (multipart: `file`, `title`, `author`, `category`, `tags`, `version`) → `202` |
| `POST` | `/api/documents/batch` | Carga masiva de hasta 20 archivos (`files` y metadatos comunes) → `202` con un resultado por archivo |
| `GET` | `/api/documents/{id}` | Detalle: metadatos, estado y contenido extraído |
| `GET` | `/api/documents/search?q=&category=&tags=&page=&size=` | Búsqueda de texto completo con resaltado y paginación; tiempo en la cabecera `Server-Timing` |
| `GET` | `/api/events` | Stream SSE: eventos `connected` y `document-status` |

Los errores siguen el formato `ProblemDetail` (RFC 7807). Sintaxis de búsqueda: `"frase exacta"`, `-excluir`, `or`.

## Pruebas

```bash
cd backend && ./mvnw verify               # unit + integración con Testcontainers (requiere Docker)
cd frontend && npx ng test --watch=false  # Vitest
```

- **CI** (GitHub Actions) en cada pull request: build y tests del backend, build y tests del frontend, y construcción de la pila completa con Docker y prueba de humo a través de nginx.
- **Colección de Bruno** en `bruno/`: ábrela con *Open Collection*, elige el entorno *Local* y ejecútala con el Runner. También desde la terminal: `cd bruno && npx @usebruno/cli run --env Local`.
- **`NoLikeOperatorTest`** hace fallar el build si el código de producción usa `LIKE`.

## Rendimiento de la búsqueda

```bash
scripts/seed/seed.sh 50000                  # documentos sintéticos (~1 min por cada 50.000)
scripts/benchmark/explain.sh rendimiento    # plan: Bitmap Index Scan sobre el índice GIN
k6 run scripts/benchmark/search-load.js     # 10 usuarios durante 1 min; falla si p95 >= 1 s
scripts/seed/clean.sh                       # elimina los documentos sintéticos
```

k6 se instala con `brew install k6`, o se ejecuta con Docker: `docker run --rm -i -e BASE_URL=http://host.docker.internal:8080 grafana/k6 run - < scripts/benchmark/search-load.js`.

Resultado con 100.007 documentos: **p95 de 373 ms, mediana de 62 ms y 84 búsquedas por segundo**, sin errores. El detalle de las mediciones y de las optimizaciones está en [docs/architecture.md](docs/architecture.md#5-motor-de-búsqueda-y-tiempos-de-respuesta).

## Variables de entorno

| Variable | Por defecto | Descripción |
| --- | --- | --- |
| `POSTGRES_DB` / `POSTGRES_USER` / `POSTGRES_PASSWORD` | `docsearch` / `docsearch` / `changeme` | Base de datos |
| `POSTGRES_PORT` | `5432` | Puerto publicado de PostgreSQL |
| `DB_URL` | `jdbc:postgresql://localhost:5432/docsearch` | URL JDBC (en Docker la fija el compose) |
| `RABBITMQ_HOST` / `RABBITMQ_PORT` | `localhost` / `5672` | Broker |
| `RABBITMQ_USER` / `RABBITMQ_PASSWORD` | `docsearch` / `changeme` | Credenciales del broker |
| `RABBITMQ_MANAGEMENT_PORT` | `15672` | Consola de RabbitMQ |
| `SERVER_PORT` | `8080` | Puerto de la API |
| `FRONTEND_PORT` | `80` | Puerto de la aplicación web (nginx) |
| `UPLOAD_MAX_FILE_SIZE` / `UPLOAD_MAX_REQUEST_SIZE` | `10MB` / `50MB` | Límites de carga |
| `STORAGE_PATH` | `./uploads` (en Docker, `/data/uploads`) | Carpeta de los archivos originales |
| `MAX_EXTRACTED_CHARS` | `5000000` | Tope de texto extraído por documento |
| `PROCESSING_CONCURRENCY` / `PROCESSING_MAX_CONCURRENCY` | `2` / `4` | Consumidores de la cola de procesamiento |
| `SSE_EMITTER_TIMEOUT` / `SSE_HEARTBEAT_INTERVAL` | `30m` / `20s` | Duración de cada stream SSE y frecuencia del heartbeat |

## Documentación

- [Arquitectura y decisiones técnicas](docs/architecture.md)
- [Uso de inteligencia artificial](docs/ia.md)
