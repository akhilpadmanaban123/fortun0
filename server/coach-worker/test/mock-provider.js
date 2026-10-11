/**
 * A stand-in for the AI provider, for testing the coach end to end without an API key.
 * It answers every request with a reply that proves what it received.
 *
 *   node test/mock-provider.js            (listens on 127.0.0.1:8799)
 *
 * Run the worker against it with OPENROUTER_BASE_URL=http://127.0.0.1:8799 in .dev.vars.
 */
const http = require('http');

const PORT = 8799;

http
  .createServer((request, response) => {
    let raw = '';
    request.on('data', (chunk) => (raw += chunk));
    request.on('end', () => {
      const body = JSON.parse(raw || '{}');
      const messages = body.messages || [];
      const question = messages[messages.length - 1]?.content || '';
      const summary = messages.find((m) => m.role === 'system' && m.content.startsWith('Summary'))?.content || '';
      const safeToSpend = /Safe to spend[^:]*:\s*(-?₹[\d,.]+)/.exec(summary)?.[1] || 'unknown';
      console.log(`[mock] model=${body.model} messages=${messages.length} question="${question}"`);
      response.writeHead(200, { 'Content-Type': 'application/json' });
      response.end(
        JSON.stringify({
          choices: [
            {
              message: {
                role: 'assistant',
                content: `(test reply) You asked: "${question}". I can see your safe-to-spend is ${safeToSpend}.`,
              },
            },
          ],
        }),
      );
    });
  })
  .listen(PORT, '127.0.0.1', () => console.log(`mock AI provider on http://127.0.0.1:${PORT}`));
