-- Seeds :count INDEXADO documents with a realistic word distribution:
--   * each document has a "topic" of 8 technical terms, so a term appears in roughly 5% of documents;
--   * the rest of the text is general vocabulary built from syllables (about 4,500 distinct words).
-- Words are built with substr() on short strings: indexing a large array per word would detoast it
-- on every access and make the seed dozens of times slower.
\set ON_ERROR_STOP on
CREATE TEMP TABLE seed_terms AS
SELECT row_number() OVER () AS n, term
FROM unnest(string_to_array(
        'arquitectura hexagonal dominio puerto adaptador servicio microservicio despliegue contenedor kubernetes docker '
            'búsqueda índice consulta rendimiento latencia caché postgresql transacción bloqueo concurrencia '
            'mensajería cola rabbitmq evento notificación configuración seguridad autenticación autorización '
            'token cifrado certificado balanceador escalabilidad disponibilidad réplica partición fragmento '
            'respaldo monitoreo métrica traza alerta observabilidad prueba unitaria integración contrato api '
            'rest graphql versión documentación manual guía especificación requisito criterio aceptación '
            'frontend backend angular spring java typescript compilación empaquetado pipeline entrega '
            'calidad revisión refactorización patrón repositorio entidad agregado comando '
            'migración esquema tabla columna clave vista procedimiento disparador permiso '
            'rol política auditoría cumplimiento normativa privacidad anonimización retención '
            'almacenamiento memoria procesador hilo asíncrono síncrono petición respuesta '
            'excepción reintento tolerancia circuito timeout limitación tráfico cuota capacidad costo '
            'optimización perfilado benchmark estrés resiliencia recuperación desastre continuidad', ' ')) AS term;

INSERT INTO documents (id, title, author, category, tags, version, original_filename, format, content_type,
                       file_size, storage_key, content, status, created_at, updated_at)
SELECT gen_random_uuid(),
       initcap(t.topic[1]) || ' de ' || t.topic[2] || ' ' || g,
       (ARRAY['Ana Pérez','Luis Gómez','María Ruiz','Carlos Díaz','Lucía Torres','Jorge Castro'])[1 + (g % 6)],
       (ARRAY['Backend','Frontend','DevOps','Arquitectura','Seguridad','Datos'])[1 + (g % 6)],
       ARRAY[t.topic[1], t.topic[2]],
       '1.' || (g % 10),
       'seed-' || g || '.txt', 'TXT', 'text/plain', 1, 'seed-' || g || '.txt',
       (SELECT string_agg(word, ' ')
          FROM (SELECT CASE WHEN random() < 0.15
                            THEN t.topic[1 + floor(random() * 8)::int]
                            ELSE substr('babebibobucacecocudadedidodufafefilalelilolumamemimomunanenono',
                                        1 + 2 * floor(power(random(), 2) * 30)::int, 2)
                              || substr('rarerirorusasesisosutatetitotu', 1 + 2 * floor(random() * 15)::int, 2)
                              || (ARRAY['l','n','r','s','do','da','ble','mente','ción','dad'])[1 + floor(random() * 10)::int]
                       END AS word
                  FROM generate_series(1, 200 + (g * 7919) % 600)) words),
       'INDEXADO',
       now() - (g || ' minutes')::interval,
       now()
FROM generate_series(1, :count) g
    CROSS JOIN LATERAL (SELECT array_agg(s.term) AS topic
    FROM (SELECT 1 + floor(random() * (SELECT count(*) FROM seed_terms))::int + 0 * g AS n
    FROM generate_series(1, 8)) pick
    JOIN seed_terms s ON s.n = pick.n) t;

ANALYZE documents;