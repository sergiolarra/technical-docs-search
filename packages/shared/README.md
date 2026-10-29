# packages/shared

Reservado para código compartido entre el backend y el frontend. **En esta versión no se usa.**

El contrato de la API es pequeño (5 endpoints) y los tipos del frontend se mantienen en
`frontend/src/app/core/models/document.model.ts`, cubiertos por los tests del frontend.

Si el contrato creciera, el siguiente paso sería generar aquí un cliente TypeScript a partir del
OpenAPI que publica el backend (`/v3/api-docs`) con OpenAPI Generator, para que un cambio de DTO en
Java rompa la compilación del frontend en lugar de fallar en ejecución.
