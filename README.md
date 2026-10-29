## Pruebas de la API

Colección de [Bruno](https://www.usebruno.com/) en `bruno/` con aserciones y archivos de ejemplo.
Ábrela con **Open Collection**, elige el entorno **Local** y ejecútala con el Runner.


## Rendimiento de la búsqueda

```bash
scripts/seed/seed.sh 50000                  # 50.000 documentos sintéticos (~1-2 min)
scripts/benchmark/explain.sh rendimiento    # plan: Bitmap Index Scan sobre el índice GIN
k6 run scripts/benchmark/search-load.js     # carga durante 1 min; falla si p95 >= 1 s
scripts/seed/clean.sh                       # elimina los documentos sintéticos
```

k6 se instala con `brew install k6`, o se ejecuta con Docker:
`docker run --rm -i -e BASE_URL=http://host.docker.internal:8080 grafana/k6 run - < scripts/benchmark/search-load.js`

## Frontend

```bash
cd frontend
npm install
npm start                    # http://localhost:4200 (proxies /api to the backend on :8080)
npx ng test --watch=false    # unit tests (Vitest)
```

## Quick start (Docker)

```bash
docker compose up --build
```

| Service | URL |
| --- | --- |
| Web app | http://localhost |
| API (direct) | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| RabbitMQ | http://localhost:15672 (docsearch / changeme) |

No `.env` is needed: every setting has a default. Copy `.env.example` to `.env` to override them.

## Development

```bash
docker compose up -d postgres rabbitmq     # infrastructure only
cd backend && ./mvnw spring-boot:run       # API on :8080
cd frontend && npm start                   # app on :4200, proxies /api to :8080
```