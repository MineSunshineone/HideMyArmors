// Run against the isolated server with the compatibility probe and keepRunning=true.
const assert = require('node:assert/strict');
const protocol = require(process.argv[2] || 'minecraft-protocol');
const clients = [];
const pause = ms => new Promise(resolve => setTimeout(resolve, ms));
async function until(check, message) {
    for (let attempt = 0; attempt < 200; attempt++) {
        if (check()) return;
        await pause(50);
    }
    throw new Error(message);
}
async function connect(username) {
    const client = protocol.createClient({ host: '127.0.0.1', port: 25598, username, version: '1.21.8', auth: 'offline' });
    clients.push(client);
    client.equipment = new Map();
    client.on('error', error => console.error(username, error.message));
    client.on('kick_disconnect', packet => console.error(username, JSON.stringify(packet)));
    client.on('login', packet => { client.entityId = packet.entityId; });
    client.on('position', packet => {
        client.write('teleport_confirm', { teleportId: packet.teleportId });
        client.write('player_loaded', {});
        client.ready = true;
    });
    client.on('entity_equipment', packet => {
        for (const entry of packet.equipments) client.equipment.set(`${packet.entityId}:${entry.slot}`, entry.item);
    });
    await until(() => client.ready, `${username} did not join`);
    return client;
}
async function headState(viewers, owner, hidden) {
    await until(() => viewers.every(viewer => {
        const item = viewer.equipment.get(`${owner.entityId}:5`);
        return item && (item.itemCount === 0) === hidden;
    }), `Expected helmet hidden=${hidden} for both viewers`);
}
async function command(client, text) {
    client.chat(text);
    await pause(400);
}
(async () => {
    let owner = await connect('ArmorOwner');
    const viewers = await Promise.all([connect('ArmorViewerA'), connect('ArmorViewerB')]);
    await pause(1500);
    await command(owner, '/hmaprobe');
    await headState(viewers, owner, false);
    await command(owner, '/hma hide all');
    await command(owner, '/hmaprobe');
    await headState(viewers, owner, true);
    owner.end();
    await pause(1500);
    owner = await connect('ArmorOwner');
    await command(owner, '/hmaprobe');
    await headState(viewers, owner, true);
    await command(owner, '/hma show all');
    await command(owner, '/hmaprobe');
    await headState(viewers, owner, false);
    await command(owner, '/hmaprobe permission');
    await pause(1500);
    await command(owner, '/hmaprobe');
    await headState(viewers, owner, true);
    assert(viewers.every(viewer => !viewer.equipment.has(`${viewer.entityId}:5`)), 'Own equipment packet was rewritten');
    console.log('CLIENT TEST PASSED: two viewers, hide/show, PDC after reconnect, permission refresh');
})().catch(error => { console.error(error); process.exitCode = 1; }).finally(() => {
    for (const client of clients) client.end();
});
