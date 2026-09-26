// MediLink PWA Service Worker
// Cache-first for the static shell (HTML/CSS/JS/icons), network for live data.
const CACHE = 'medilink-v1';
const ASSETS = [
    'landing.html',
    'index.html',
    'style.css',
    'app.js',
    'manifest.json',
    'icons/icon-192.png',
    'icons/icon-512.png'
];

self.addEventListener('install', e => {
    e.waitUntil(
        caches.open(CACHE)
            .then(c => c.addAll(ASSETS))
            .then(() => self.skipWaiting())
    );
});

self.addEventListener('activate', e => {
    e.waitUntil(
        caches.keys()
            .then(keys => Promise.all(keys.filter(k => k !== CACHE).map(k => caches.delete(k))))
            .then(() => self.clients.claim())
    );
});

self.addEventListener('fetch', e => {
    const url = new URL(e.request.url);

    // Never touch cross-origin (Google Fonts, Tesseract CDN) or live backend data
    if (url.origin !== location.origin) return;
    if (url.pathname.startsWith('/api/') || url.pathname.startsWith('/ws/')) return;
    if (url.pathname.startsWith('/h2-console')) return;
    if (e.request.method !== 'GET') return;

    e.respondWith(
        caches.match(e.request).then(hit => {
            if (hit) return hit;
            return fetch(e.request).then(res => {
                if (res.ok) {
                    const copy = res.clone();
                    caches.open(CACHE).then(c => c.put(e.request, copy));
                }
                return res;
            }).catch(() => caches.match('index.html'));
        })
    );
});
