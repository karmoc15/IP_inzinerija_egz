const express = require('express');
const { SerialPort } = require('serialport');
const { ReadlineParser } = require('@serialport/parser-readline');
const cors = require('cors');
const { WebSocketServer } = require('ws');
const path = require('path');

const app = express();
app.use(cors());
app.use(express.json());

// Serve static files
app.use(express.static(path.join(__dirname, 'public')));

// Serial port setup
const port = new SerialPort({
  path: 'COM6',
  baudRate: 9600
});
const parser = port.pipe(new ReadlineParser({ delimiter: '\n' }));

// WebSocket server setup
const wss = new WebSocketServer({ noServer: true });

let ldrValue = 0;
let thresholds = { dark: 300, bright: 700 };

// Broadcast function for WebSocket
const broadcast = (message) => {
  wss.clients.forEach(client => {
    if (client.readyState === client.OPEN) {
      client.send(JSON.stringify(message));
    }
  });
};

// Read data from Arduino
parser.on('data', (data) => {
  const match = data.trim().match(/LDR:\s*(\d+)/);
  if (match) {
    ldrValue = parseInt(match[1], 10);
    //console.log(`New LDR value: ${ldrValue}`);
    // Broadcast the updated LDR value to all WebSocket clients
    broadcast({ ldrValue });
  }
});

// REST API Endpoints
app.get('/ldr', (req, res) => {
  res.json({ ldrValue, thresholds });
});

app.post('/thresholds', (req, res) => {
  thresholds.dark = req.body.dark;
  thresholds.bright = req.body.bright;

  // Send thresholds to Arduino
  port.write(`dark:${thresholds.dark}\n`);
  port.write(`bright:${thresholds.bright}\n`);

  // Broadcast updated thresholds to WebSocket clients
  broadcast({ thresholds });

  res.json({ success: true });
});


// WebSocket upgrade
const server = app.listen(3000, () => {
  console.log('Server running on http://localhost:3000');
});
server.on('upgrade', (request, socket, head) => {
  wss.handleUpgrade(request, socket, head, (ws) => {
    wss.emit('connection', ws, request);
  });
});
