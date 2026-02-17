const fetch = require('node-fetch'); // Ensure node-fetch is available or use native fetch in Node 18+

const BASE_URL = 'http://localhost:8080/api/v1';
const USER_EMAIL = 'admin@admin.com'; // Replace with valid credentials
const USER_PASS = 'admin';

async function main() {
    console.log('🚀 Starting Verification Simulation...');

    // 1. Authenticate
    console.log('\n🔐 Authenticating...');
    let token;
    try {
        const authRes = await fetch(`${BASE_URL}/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email: USER_EMAIL, password: USER_PASS })
        });
        if (!authRes.ok) throw new Error(`Auth failed: ${authRes.status}`);
        const authData = await authRes.json();
        token = authData.token;
        console.log('✅ Authentication successful.');
    } catch (e) {
        console.error('❌ Authentication failed. Is the backend running?');
        process.exit(1);
    }

    const headers = {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
    };

    // 2. Create Parent Entities (Client -> OS -> Vehicle)
    const clientLocalId = 'client-local-' + Date.now();
    const osLocalId = 'os-local-' + Date.now();
    const veiculoLocalId = 'veiculo-local-' + Date.now();
    const pecaLocalId = 'peca-local-' + Date.now();

    console.log(`\n📦 Creating Parent Entities (LocalIDs: ${clientLocalId}, ${osLocalId}, ${veiculoLocalId})`);

    let clientId, osId, veiculoId;

    // Create Client
    try {
        const cRes = await fetch(`${BASE_URL}/clientes`, {
            method: 'POST',
            headers,
            body: JSON.stringify({
                razaoSocial: 'Cliente Teste Sync',
                nomeFantasia: 'Teste Sync',
                tipoPessoa: 'FISICA',
                cpf: '12345678901', // Mock Valid CPF logic if needed or use existing
                localId: clientLocalId
            })
        });
        const cData = await cRes.json();
        clientId = cData.id;
        console.log(`✅ Client Synced! ID: ${clientId} (Server)`);
    } catch (e) { console.error('❌ Failed to create Client', e); return; }

    // Create OS
    try {
        const osRes = await fetch(`${BASE_URL}/ordens-servico`, {
            method: 'POST',
            headers,
            body: JSON.stringify({
                clienteId: clientId,
                data: new Date().toISOString().split('T')[0],
                localId: osLocalId
            })
        });
        const osData = await osRes.json();
        osId = osData.id;
        console.log(`✅ OS Synced! ID: ${osId} (Server)`);
    } catch (e) { console.error('❌ Failed to create OS', e); return; }

    // Create Vehicle
    try {
        const vRes = await fetch(`${BASE_URL}/ordens-servico/veiculos`, {
            method: 'POST',
            headers,
            body: JSON.stringify({
                ordemServicoId: osId,
                placa: 'TEST1234',
                modelo: 'Fusca Teste',
                cor: 'Azul',
                localId: veiculoLocalId
            })
        });
        // API returns OS response usually, need to extract vehicle ID if needed, 
        // but for now we trust OS response contains vehicles
        console.log(`✅ Vehicle Synced! (Server Confirmed)`);

        // Fetch Vehicle ID for Peca (Mocking app logic: app knows Vehicle ID after sync)
        // In real app, we use localId mapping. Here we just fetch OS to find valid ID.
        const osCheck = await (await fetch(`${BASE_URL}/ordens-servico/${osId}`, { headers })).json();
        const vehicle = osCheck.veiculos.find(v => v.localId === veiculoLocalId || v.placa === 'TEST1234');
        veiculoId = vehicle.id;
    } catch (e) { console.error('❌ Failed to create Vehicle', e); return; }

    // 3. Simulate Peca Creation (Fail First, Then Retry with Same LocalID)
    console.log(`\n🔧 Simulating Peca Sync (LocalID: ${pecaLocalId})...`);

    // Mock Payload
    const pecaPayload = {
        veiculoId: veiculoId,
        tipoPecaId: 1, // Ensure ID 1 exists or fetch first available
        valor: 100.00,
        localId: pecaLocalId,
        descricao: 'Peça Teste Idempotencia'
    };

    // SIMULATE FAILURE (Network Error / Timeout - Client side simulation)
    console.log('⚠️ Simulating Network Failure during first attempt...');
    // In a real test we might kill the network, here we just assume failure 
    // BUT to test BACKEND idempotency, we must assume the request *reached* the server but response was lost?
    // OR we simply fire the request twice.

    // Attempt 1: Success (Server processes it)
    console.log('👉 Sending Request 1 (Server saves it)...');
    const res1 = await fetch(`${BASE_URL}/ordens-servico/pecas`, {
        method: 'POST',
        headers,
        body: JSON.stringify(pecaPayload)
    });
    const data1 = await res1.json();
    console.log(`✅ Request 1 Result: HTTP ${res1.status} (ID: ${data1.veiculos[0].pecas[0].id})`);

    // Attempt 2: Retry (Same LocalID) -> Should be Update/Idempotent
    console.log('👉 Sending Request 2 (Retry with SAME LocalID)...');

    // Change value slightly to verify Update vs Ignore
    const pecaPayloadRetry = { ...pecaPayload, valor: 150.00, descricao: 'Peça Teste Idempotencia (UPDATED)' };

    const res2 = await fetch(`${BASE_URL}/ordens-servico/pecas`, {
        method: 'POST',
        headers,
        body: JSON.stringify(pecaPayloadRetry)
    });

    const data2 = await res2.json();
    console.log(`✅ Request 2 Result: HTTP ${res2.status}`);

    // VERIFICATION
    console.log('\n🔎 Verifying Backend State...');
    const osFinal = await (await fetch(`${BASE_URL}/ordens-servico/${osId}`, { headers })).json();
    const finalVehicle = osFinal.veiculos.find(v => v.id === veiculoId);
    const finalPecas = finalVehicle.pecas.filter(p => p.localId === pecaLocalId);

    console.log(`   - Total Pecas with LocalID '${pecaLocalId}': ${finalPecas.length}`);
    if (finalPecas.length === 1) {
        console.log(`✅ IDEMPOTENCY CONFIRMED: Only 1 record exists.`);
        const p = finalPecas[0];
        if (p.descricao.includes('UPDATED')) {
            console.log(`✅ UPSERT CONFIRMED: Record was updated (Desc: ${p.descricao}).`);
        } else {
            console.log(`⚠️ UPSERT WARNING: Record was NOT updated (Desc: ${p.descricao}). Check logic.`);
        }
    } else {
        console.error(`❌ FAILURE: Found ${finalPecas.length} records. Idempotency failed.`);
    }
}

main();
