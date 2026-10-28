# Guía de la API de documentos

## Arquitectura

El backend sigue una **arquitectura hexagonal** con Spring Boot. El dominio no depende de frameworks.

## Búsqueda

La búsqueda de texto completo usa PostgreSQL con `tsvector` e índice GIN.

- Carga asíncrona con RabbitMQ
- Extracción de texto con Apache Tika
- Notificaciones en tiempo real con SSE
