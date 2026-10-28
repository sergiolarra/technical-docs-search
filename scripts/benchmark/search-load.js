import http from 'k6/http';
import { check } from 'k6';
import { Trend } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// Search time measured by the backend itself (Server-Timing header), without network or queueing
const serverSearchTime = new Trend('server_search_time', true);

// Mix of very common, medium and rare terms, phrases and exclusions
const QUERIES = [
    'arquitectura', 'rendimiento', 'kubernetes', 'seguridad token', 'observabilidad',
    'latencia caché', 'migración esquema', '"recuperación desastre"', 'arquitectura -kubernetes', 'búsqueda índice',
];

export const options = {
    scenarios: {
        search: {
            executor: 'constant-vus',
            vus: Number(__ENV.VUS || 10),
            duration: __ENV.DURATION || '1m',
        },
    },
    // The run fails if the 1 s budget is not met
    thresholds: {
        http_req_failed: ['rate<0.01'],
        http_req_duration: ['p(95)<1000'],
        server_search_time: ['p(95)<1000'],
    },
};

export default function () {
    const q = QUERIES[Math.floor(Math.random() * QUERIES.length)];
    const page = Math.random() < 0.8 ? 0 : Math.floor(Math.random() * 5);
    const res = http.get(`${BASE_URL}/api/documents/search?q=${encodeURIComponent(q)}&page=${page}&size=10`, {
        tags: { name: 'search' },
    });

    check(res, { 'status is 200': (r) => r.status === 200 });

    const timing = /dur=([\d.]+)/.exec(res.headers['Server-Timing'] || '');
    if (timing) {
        serverSearchTime.add(parseFloat(timing[1]));
    }
}