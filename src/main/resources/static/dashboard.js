/* ============================================================
   MediLink simple dashboard logic.
   Plain fetch + render — one function per feature, no frameworks.
   ============================================================ */

// ---------- Session ----------
let me = null;
try { me = JSON.parse(sessionStorage.getItem('medilink_user') || 'null'); } catch (e) { me = null; }

async function requireLogin() {
    if (me) return true;
    const r = await fetch('/api/reminders');
    if (r.status === 401) { location.href = 'login.html'; return false; }
    return true;
}

function logout() {
    fetch('/api/auth/logout', { method: 'POST' }).finally(() => {
        sessionStorage.removeItem('medilink_user');
        location.href = 'landing.html';
    });
}

// ---------- Navigation ----------
const NAV = [
    { id: 'home',          icon: '🏠', label: 'Home',           roles: ['PATIENT', 'PHARMACIST', 'ADMIN'] },
    { id: 'medicines',     icon: '💊', label: 'Medicines',      roles: ['PATIENT', 'PHARMACIST', 'ADMIN'] },
    { id: 'emergency',     icon: '🚨', label: 'Emergency',      roles: ['PATIENT', 'PHARMACIST', 'ADMIN'] },
    { id: 'prescriptions', icon: '📄', label: 'Prescriptions',  roles: ['PATIENT'] },
    { id: 'verify',        icon: '🛡️', label: 'Verify Medicine', roles: ['PATIENT', 'PHARMACIST', 'ADMIN'] },
    { id: 'reminders',     icon: '⏰', label: 'Reminders',      roles: ['PATIENT'] },
    { id: 'chat',          icon: '💬', label: 'Chat',           roles: ['PATIENT', 'PHARMACIST'] },
    { id: 'stock',         icon: '📦', label: 'Stock',          roles: ['PHARMACIST'] },
    { id: 'profile',       icon: '⚙️', label: 'Profile',        roles: ['PATIENT', 'PHARMACIST', 'ADMIN'] }
];

function buildNav() {
    const role = me ? me.role : 'PATIENT';
    const nav = document.getElementById('sidenav');
    nav.innerHTML = NAV.filter(n => n.roles.includes(role))
        .map(n => `<button id="nav-${n.id}" onclick="showSection('${n.id}')">${n.icon} <span>${n.label}</span></button>`)
        .join('');
}

function showSection(id) {
    document.querySelectorAll('.section').forEach(s => s.classList.remove('active'));
    const sec = document.getElementById('sec-' + id);
    if (sec) sec.classList.add('active');
    document.querySelectorAll('.sidenav button').forEach(b => b.classList.remove('active'));
    const btn = document.getElementById('nav-' + id);
    if (btn) btn.classList.add('active');
    window.scrollTo(0, 0);
    if (id === 'home') renderHome();
    if (id === 'stock') loadStocks();
    if (id === 'prescriptions') loadPrescriptions();
    if (id === 'reminders') loadReminders();
    if (id === 'chat') loadChat();
    if (id === 'emergency') loadEmergency();
}

// ---------- Toast ----------
let toastTimer = null;
function toast(text) {
    const t = document.getElementById('toast');
    t.textContent = text;
    t.classList.add('show');
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => t.classList.remove('show'), 3200);
}

// ---------- Home ----------
function renderHome() {
    const name = me ? me.name : 'friend';
    const role = me ? me.role : 'PATIENT';
    document.getElementById('home-greeting').textContent =
        role === 'PHARMACIST' ? `Welcome, Pharmacist ${name}` : `Welcome, ${name}`;
    document.getElementById('home-sub').textContent =
        role === 'PHARMACIST'
            ? 'Manage your stock and answer patient questions in real time.'
            : 'Search medicines, scan prescriptions, and find emergency stock near you.';

    const roleLabel = role === 'PHARMACIST' ? 'Pharmacist' : role === 'ADMIN' ? 'Administrator' : 'Patient';
    document.getElementById('user-badge').innerHTML = `${name} · <b>${roleLabel}</b>`;

    Promise.all([
        fetch('/api/medicines').then(r => r.json()).catch(() => ({})),
        fetch('/api/pharmacies/emergency').then(r => r.json()).catch(() => ({}))
    ]).then(([meds, em]) => {
        const cards = [
            { num: (meds.results || []).length, lbl: 'Medicines in database' },
            { num: (em.emergencyPharmacies || []).length, lbl: 'Partner pharmacies' }
        ];
        if (role === 'PATIENT') {
            cards.push({ num: '24/7', lbl: 'Emergency finder' });
            cards.push({ num: 'OCR', lbl: 'Prescription scanner' });
        } else {
            cards.push({ num: 'LIVE', lbl: 'Stock sync' });
            cards.push({ num: 'CHAT', lbl: 'Patient consults' });
        }
        document.getElementById('stat-grid').innerHTML = cards.map(c =>
            `<div class="stat-card"><div class="num">${c.num}</div><div class="lbl">${c.lbl}</div></div>`).join('');
    });

    const actions = {
        PATIENT: [
            ['💊 Search medicines', 'medicines'], ['🚨 Emergency finder', 'emergency'],
            ['📄 Scan prescription', 'prescriptions'], ['🛡️ Verify batch', 'verify']
        ],
        PHARMACIST: [
            ['📦 Update stock', 'stock'], ['💬 Answer patients', 'chat'],
            ['💊 Browse medicines', 'medicines']
        ],
        ADMIN: [['💊 Browse medicines', 'medicines'], ['🛡️ Verify batches', 'verify']]
    };
    document.getElementById('quick-actions').innerHTML = (actions[role] || actions.PATIENT)
        .map(([lbl, sec]) => `<button class="btn-primary" onclick="showSection('${sec}')">${lbl}</button>`)
        .join('');
}

// ---------- Medicines ----------
async function searchMedicines() {
    const q = document.getElementById('med-query').value.trim();
    const strategy = document.getElementById('med-strategy').value;
    if (!q) { toast('Type a medicine name first.'); return; }
    const box = document.getElementById('med-results');
    box.innerHTML = '<div class="card muted">Searching...</div>';
    document.getElementById('price-panel').innerHTML = '';
    try {
        const res = await fetch(`/api/medicines/search?query=${encodeURIComponent(q)}&strategy=${strategy}`);
        const data = await res.json();
        const results = data.results || [];
        if (results.length === 0) {
            box.innerHTML = '<div class="card muted">No medicines matched your search.</div>';
            return;
        }
        box.innerHTML = results.map(m => `
            <div class="card med-card">
                <div class="row1">
                    <div>
                        <strong style="font-size:1.05rem;">${m.brandName} ${m.strength}</strong>
                        <div class="meta">${m.genericName} · ${m.company}</div>
                        <span class="tag">${m.formulation}</span>
                        <span class="tag">DGDA Verified</span>
                    </div>
                    <div class="price">৳${m.unitPrice}</div>
                </div>
                ${m.sideEffects ? `<div class="meta" style="margin:6px 0 10px;">${m.sideEffects}</div>` : ''}
                <button class="btn-ghost" onclick="showPrices(${m.id})">🏪 Compare pharmacy prices</button>
            </div>`).join('');
    } catch (e) {
        box.innerHTML = '<div class="card muted">Could not reach the server.</div>';
    }
}

async function showPrices(medicineId) {
    const panel = document.getElementById('price-panel');
    panel.innerHTML = '<div class="card muted">Loading prices...</div>';
    try {
        const res = await fetch(`/api/medicines/pharmacy-prices?medicineId=${medicineId}`);
        const d = await res.json();
        if (d.status !== 'SUCCESS') { panel.innerHTML = '<div class="card muted">No pharmacy prices found.</div>'; return; }
        panel.innerHTML = `
            <div class="card">
                <h2>Pharmacy prices — ${d.brandName} ${d.strength || ''}</h2>
                <p class="muted" style="margin-bottom:10px;">
                    Best price <b style="color:var(--green);">৳${d.bestPrice}</b> · Highest ৳${d.maxPrice}
                    · You save up to <b>${d.savingsPercent}%</b> (MRP ৳${d.basePrice})
                </p>
                <table class="data-table">
                    <thead><tr><th>Pharmacy</th><th>Area</th><th>Price</th><th>Stock</th></tr></thead>
                    <tbody>
                        ${(d.pharmacyPrices || []).map(p => `
                            <tr>
                                <td>${p.pharmacyName} ${p.isBestPrice ? '<span class="badge badge24">BEST</span>' : ''}</td>
                                <td class="muted">${p.area}</td>
                                <td><b>৳${p.unitPrice}</b></td>
                                <td>${p.quantity} units ${p.is24Hours ? '<span class="badge badge24">24h</span>' : ''}</td>
                            </tr>`).join('')}
                    </tbody>
                </table>
            </div>`;
    } catch (e) {
        panel.innerHTML = '<div class="card muted">Could not load prices.</div>';
    }
}

// ---------- Emergency ----------
function loadEmergency() {
    const [lat, lng] = (document.getElementById('em-area').value || '23.7465,90.3760').split(',');
    fetchEmergency(lat, lng);
}

async function fetchEmergency(lat, lng) {
    const box = document.getElementById('em-list');
    box.innerHTML = '<div class="card muted">Finding pharmacies...</div>';
    try {
        const res = await fetch(`/api/pharmacies/emergency?lat=${lat}&lng=${lng}`);
        const data = await res.json();
        const list = data.emergencyPharmacies || [];
        // map: box around user + nearest pharmacy, marker on the pharmacy
        const map = document.getElementById('em-map');
        if (list.length > 0) {
            const n = list[0];
            const pad = 0.012;
            const minLat = Math.min(parseFloat(lat), n.lat) - pad;
            const maxLat = Math.max(parseFloat(lat), n.lat) + pad;
            const minLng = Math.min(parseFloat(lng), n.lng) - pad;
            const maxLng = Math.max(parseFloat(lng), n.lng) + pad;
            map.src = 'https://www.openstreetmap.org/export/embed.html'
                + `?bbox=${minLng.toFixed(5)}%2C${minLat.toFixed(5)}%2C${maxLng.toFixed(5)}%2C${maxLat.toFixed(5)}`
                + `&layer=mapnik&marker=${n.lat}%2C${n.lng}`;
        } else {
            map.removeAttribute('src');
        }
        box.innerHTML = list.map(p => `
            <div class="card pharm-card">
                <div class="row1">
                    <div>
                        <strong>${p.name}</strong>
                        <div class="meta">${p.address} (${p.area})</div>
                        <span class="badge ${p.is24Hours ? 'badge24' : 'badgenorm'}">${p.is24Hours ? '🕒 Open 24 Hours' : '🕒 Regular hours'}</span>
                    </div>
                    <div class="price" style="color:var(--blue);">${p.distanceKm} km</div>
                </div>
                <div class="actions">
                    <a class="btn-primary" style="text-decoration:none;" href="tel:${p.phone}">📞 Call</a>
                    <a class="btn-ghost" style="text-decoration:none;" target="_blank" rel="noopener"
                       href="https://www.google.com/maps/dir/?api=1&destination=${p.lat},${p.lng}&travelmode=driving">🧭 Directions</a>
                </div>
            </div>`).join('');
    } catch (e) {
        box.innerHTML = '<div class="card muted">Could not reach the server.</div>';
    }
}

function useMyLocation() {
    if (!navigator.geolocation) { toast('GPS not supported on this device.'); return; }
    toast('📡 Getting your location...');
    navigator.geolocation.getCurrentPosition(pos => {
        const lat = pos.coords.latitude.toFixed(5);
        const lng = pos.coords.longitude.toFixed(5);
        const sel = document.getElementById('em-area');
        let opt = sel.querySelector('option[data-gps]');
        if (!opt) {
            opt = document.createElement('option');
            opt.setAttribute('data-gps', '1');
            sel.insertBefore(opt, sel.firstChild);
        }
        opt.value = lat + ',' + lng;
        opt.textContent = `📍 My GPS location (${lat}, ${lng})`;
        sel.value = opt.value;
        fetchEmergency(lat, lng);
        toast('✅ Location locked — showing nearest pharmacies.');
    }, err => toast('⚠️ Could not get location: ' + err.message),
        { enableHighAccuracy: true, timeout: 10000 });
}

// ---------- Prescriptions ----------
async function loadPrescriptions() {
    const box = document.getElementById('rx-list');
    box.innerHTML = '<div class="muted">Loading...</div>';
    try {
        const res = await fetch('/api/prescriptions');
        const data = await res.json();
        const list = data.prescriptions || [];
        if (list.length === 0) {
            box.innerHTML = '<div class="muted">No prescriptions yet — upload your first one above.</div>';
            return;
        }
        box.innerHTML = list.map(rx => `
            <div class="list-item">
                <div>
                    <b>${rx.doctorName}</b> <span class="muted">· ${rx.hospital}</span><br>
                    <span class="muted" style="font-size:0.82rem;">${(rx.items || []).map(i => i.brandName).join(', ') || 'No medicines detected'}</span>
                </div>
                <div style="display:flex; gap:8px; align-items:center;">
                    <span class="status-pill st-${(rx.status || '').toLowerCase()}">${rx.status}</span>
                    ${rx.status !== 'VERIFIED' ? `<button class="btn-ghost" onclick="advanceRx(${rx.id})">Advance →</button>` : ''}
                    <button class="btn-ghost" onclick="deleteRx(${rx.id})">🗑</button>
                </div>
            </div>`).join('');
    } catch (e) {
        box.innerHTML = '<div class="muted">Could not load prescriptions.</div>';
    }
}

async function savePrescription() {
    const text = document.getElementById('rx-text').value.trim();
    if (!text) { toast('Upload a photo or type the prescription text first.'); return; }
    const res = await fetch('/api/prescriptions/upload', {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ scanText: text })
    });
    const data = await res.json();
    if (data.status === 'SUCCESS') {
        toast('✅ Prescription saved and medicines matched.');
        document.getElementById('rx-text').value = '';
        document.getElementById('rx-ocr-status').textContent = '';
        loadPrescriptions();
    } else {
        toast('⚠️ ' + (data.message || 'Could not save.'));
    }
}

async function advanceRx(id) {
    await fetch('/api/prescriptions/advance', {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ prescriptionId: String(id) })
    });
    loadPrescriptions();
}

async function deleteRx(id) {
    await fetch('/api/prescriptions/delete', {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ prescriptionId: String(id) })
    });
    loadPrescriptions();
}

// OCR: read the uploaded photo with Tesseract in the browser
document.addEventListener('DOMContentLoaded', () => {
    const input = document.getElementById('rx-file');
    if (input) {
        input.addEventListener('change', async () => {
            const file = input.files[0];
            if (!file) return;
            const status = document.getElementById('rx-ocr-status');
            if (typeof Tesseract === 'undefined') {
                status.textContent = 'OCR engine unavailable — type the text manually below.';
                return;
            }
            status.textContent = '🔍 Reading prescription with OCR...';
            try {
                const result = await Tesseract.recognize(file, 'eng');
                document.getElementById('rx-text').value = result.data.text.trim();
                status.textContent = '✅ Text extracted — check it, then press Save.';
            } catch (e) {
                status.textContent = '⚠️ OCR failed — type the text manually below.';
            }
        });
    }
});

// ---------- Verify ----------
async function verifyCode() {
    const code = document.getElementById('verify-code').value.trim();
    const box = document.getElementById('verify-result');
    if (!code) { toast('Enter the batch/QR code first.'); return; }
    box.innerHTML = '<div class="card muted">Checking DGDA registry...</div>';
    try {
        const res = await fetch('/api/medicines/verify', {
            method: 'POST', headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ code: code })
        });
        const d = await res.json();
        const expired = !d.isAuthentic && (d.details || '').toUpperCase().includes('EXPIRED');
        box.innerHTML = d.isAuthentic
            ? `<div class="card verdict ok"><div class="big">✅ GENUINE MEDICINE</div>
               <div class="muted" style="margin-top:6px;">${d.details} Manufacturer: <b>${d.manufacturer}</b></div></div>`
            : expired
                ? `<div class="card verdict bad"><div class="big">⚠️ EXPIRED BATCH</div>
                   <div class="muted" style="margin-top:6px;">${d.details} Manufacturer: <b>${d.manufacturer}</b></div></div>`
                : `<div class="card verdict bad"><div class="big">❌ SUSPECTED FAKE</div>
                   <div class="muted" style="margin-top:6px;">${d.details} Source: <b>${d.manufacturer}</b></div></div>`;
    } catch (e) {
        box.innerHTML = '<div class="card muted">Could not reach the server.</div>';
    }
}

// ---------- Reminders ----------
async function loadReminders() {
    const box = document.getElementById('rem-list');
    box.innerHTML = '<div class="muted">Loading...</div>';
    try {
        const res = await fetch('/api/reminders');
        const data = await res.json();
        const list = data.reminders || [];
        box.innerHTML = list.length === 0
            ? '<div class="muted">No reminders yet — add one above.</div>'
            : list.map(r => `
                <div class="list-item">
                    <div><b>${r.medicine}</b> <span class="muted">${r.dosage}</span><br>
                    <span class="muted" style="font-size:0.82rem;">⏰ ${r.time} · ${r.frequency}</span></div>
                    <span class="badge ${r.active ? 'badge24' : 'badgenorm'}">${r.active ? 'Active' : 'Paused'}</span>
                </div>`).join('');
    } catch (e) {
        box.innerHTML = '<div class="muted">Could not load reminders.</div>';
    }
}

async function createReminder() {
    const medicine = document.getElementById('rem-med').value.trim();
    if (!medicine) { toast('Enter the medicine name.'); return; }
    const body = {
        medicine: medicine,
        dosage: document.getElementById('rem-dose').value.trim(),
        time: document.getElementById('rem-time').value || '08:00',
        frequency: document.getElementById('rem-freq').value,
        instructions: ''
    };
    const res = await fetch('/api/reminders/create', {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body)
    });
    const data = await res.json();
    if (data.status === 'SUCCESS') {
        toast('⏰ Reminder set — you will get an alert at ' + body.time);
        document.getElementById('rem-med').value = '';
        document.getElementById('rem-dose').value = '';
        loadReminders();
    } else {
        toast('⚠️ ' + (data.message || 'Could not save.'));
    }
}

async function testAlarm() {
    await fetch('/api/reminders/test-alert', { method: 'POST' });
    toast('🔔 Test alarm sent — watch for the MEDICINE ALARM push.');
}

// ---------- Chat ----------
async function loadChat() {
    const box = document.getElementById('chat-box');
    try {
        const res = await fetch('/api/chat/messages');
        const data = await res.json();
        const list = data.messages || [];
        const iAmPatient = me && me.role === 'PATIENT';
        document.getElementById('chat-with').textContent = iAmPatient
            ? 'Chatting with your pharmacist — messages are instant.'
            : 'Answering patient questions — replies are instant.';
        box.innerHTML = list.map(m => {
            const mine = me && m.senderRole === me.role;
            return `<div class="chat-msg ${mine ? 'mine' : 'theirs'}">
                        <span class="who">${m.senderName}</span>${escapeHtml(m.content)}</div>`;
        }).join('') || '<div class="muted">No messages yet — say hello!</div>';
        box.scrollTop = box.scrollHeight;
    } catch (e) { /* ignore */ }
}

async function sendChat() {
    const input = document.getElementById('chat-input');
    const text = input.value.trim();
    if (!text) return;
    input.value = '';
    await fetch('/api/chat/send', {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ content: text })
    });
    loadChat();
}

function escapeHtml(s) {
    return String(s == null ? '' : s).replace(/[&<>"']/g, c =>
        ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}

// ---------- Stock (pharmacist) ----------
let stockRows = [];
async function loadStocks() {
    try {
        const res = await fetch('/api/pharmacies/stocks');
        const data = await res.json();
        stockRows = data.stocks || [];
        renderStocks();
    } catch (e) { /* ignore */ }
}

function renderStocks() {
    const filter = (document.getElementById('stock-filter').value || '').toLowerCase();
    const body = document.getElementById('stock-body');
    const rows = stockRows.filter(s =>
        !filter || s.medicineBrandName.toLowerCase().includes(filter) || s.pharmacyName.toLowerCase().includes(filter));
    body.innerHTML = rows.map(s => `
        <tr>
            <td><b>${s.medicineBrandName}</b><br><span class="muted" style="font-size:0.78rem;">${s.genericName}</span></td>
            <td>${s.pharmacyName}</td>
            <td><input type="number" min="0" value="${s.quantity}" id="qty-${s.id}"></td>
            <td><button class="btn-primary" style="padding:7px 14px;" onclick="saveStock(${s.id})">Save</button></td>
        </tr>`).join('') || '<tr><td colspan="4" class="muted">No stock rows.</td></tr>';
}

async function saveStock(id) {
    const qty = document.getElementById('qty-' + id).value;
    const res = await fetch('/api/pharmacies/stock/update', {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ stockId: String(id), quantity: parseInt(qty || '0', 10) })
    });
    const data = await res.json();
    if (data.status === 'SUCCESS') toast('✅ Stock updated — all users see it live.');
    else toast('⚠️ ' + (data.message || 'Update failed.'));
}

// ---------- Profile ----------
async function loadProfile() {
    if (!me) return;
    document.getElementById('pf-name').value = me.name || '';
    document.getElementById('pf-phone').value = me.phone || '';
}

async function saveProfile() {
    const name = document.getElementById('pf-name').value.trim();
    const phone = document.getElementById('pf-phone').value.trim();
    const res = await fetch('/api/patient/profile', {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ name: name, phone: phone })
    });
    const data = await res.json();
    if (data.status === 'SUCCESS' || data.name) {
        me.name = name; me.phone = phone;
        sessionStorage.setItem('medilink_user', JSON.stringify(me));
        document.getElementById('pf-msg').textContent = '✅ Profile saved.';
        renderHome();
    } else {
        document.getElementById('pf-msg').textContent = '⚠️ ' + (data.message || 'Could not save.');
    }
}

// ---------- Live events (SSE) ----------
let chatPollTimer = null;

function connectEvents() {
    try {
        const es = new EventSource('/api/events/stream');
        es.onmessage = ev => {
            const msg = ev.data || '';
            if (msg.startsWith('STOCK_UPDATE')) {
                if (document.getElementById('sec-stock').classList.contains('active')) loadStocks();
                toast('📦 ' + msg.replace('STOCK_UPDATE: ', ''));
            } else if (msg.startsWith('CHAT_MESSAGE')) {
                if (document.getElementById('sec-chat').classList.contains('active')) loadChat();
            } else if (msg.startsWith('MEDICINE ALARM') || msg.startsWith('REMINDER')) {
                toast('🔔 ' + msg);
            }
        };
    } catch (e) { /* live events are optional */ }

    // Polling fallback: keeps the chat thread synced even if the SSE stream
    // is dropped (proxy, sleep, reconnect). Only fetches while the tab is open.
    if (!chatPollTimer) {
        chatPollTimer = setInterval(() => {
            if (document.getElementById('sec-chat').classList.contains('active')) loadChat();
        }, 3000);
    }
}

// ---------- Boot ----------
document.addEventListener('DOMContentLoaded', async () => {
    if (!await requireLogin()) return;
    buildNav();
    loadProfile();
    showSection('home');
    connectEvents();
});
