package com.tutorial.hello.handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class ApiConsoleHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        String html = """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Hello Service API Console</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, sans-serif;
            background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
            min-height: 100vh;
            padding: 20px;
        }
        .container {
            max-width: 1200px;
            margin: 0 auto;
            background: white;
            border-radius: 12px;
            box-shadow: 0 20px 60px rgba(0,0,0,0.3);
            overflow: hidden;
        }
        .header {
            background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
            color: white;
            padding: 30px;
            text-align: center;
        }
        .header h1 { font-size: 2.5em; margin-bottom: 10px; }
        .header p { opacity: 0.9; font-size: 1.1em; }
        .content { padding: 30px; }
        .endpoint {
            background: #f8f9fa;
            border-radius: 8px;
            padding: 20px;
            margin-bottom: 20px;
            border-left: 4px solid #667eea;
        }
        .endpoint-header {
            display: flex;
            align-items: center;
            gap: 15px;
            margin-bottom: 15px;
        }
        .method {
            padding: 6px 12px;
            border-radius: 4px;
            font-weight: bold;
            font-size: 0.85em;
            text-transform: uppercase;
        }
        .method.get { background: #28a745; color: white; }
        .method.post { background: #007bff; color: white; }
        .path {
            font-family: 'Courier New', monospace;
            font-size: 1.1em;
            font-weight: 600;
            color: #333;
        }
        .description { color: #666; margin-bottom: 15px; line-height: 1.6; }
        .input-group {
            margin-bottom: 15px;
        }
        .input-group label {
            display: block;
            margin-bottom: 5px;
            font-weight: 600;
            color: #555;
        }
        textarea, input {
            width: 100%%;
            padding: 10px;
            border: 1px solid #ddd;
            border-radius: 4px;
            font-family: 'Courier New', monospace;
            font-size: 0.9em;
        }
        textarea { min-height: 100px; resize: vertical; }
        button {
            background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
            color: white;
            border: none;
            padding: 12px 24px;
            border-radius: 6px;
            cursor: pointer;
            font-weight: 600;
            font-size: 1em;
            transition: transform 0.2s, box-shadow 0.2s;
        }
        button:hover {
            transform: translateY(-2px);
            box-shadow: 0 5px 15px rgba(102, 126, 234, 0.4);
        }
        button:active { transform: translateY(0); }
        .response {
            margin-top: 15px;
            padding: 15px;
            background: #f1f3f5;
            border-radius: 6px;
            border-left: 4px solid #28a745;
        }
        .response.error { border-left-color: #dc3545; }
        .response pre {
            margin: 0;
            font-family: 'Courier New', monospace;
            white-space: pre-wrap;
            word-wrap: break-word;
            font-size: 0.9em;
        }
        .status {
            display: inline-block;
            padding: 4px 8px;
            border-radius: 4px;
            font-weight: 600;
            font-size: 0.85em;
            margin-bottom: 10px;
        }
        .status.success { background: #d4edda; color: #155724; }
        .status.error { background: #f8d7da; color: #721c24; }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>🚀 Hello Service API</h1>
            <p>Interactive API Console - Test your endpoints here</p>
        </div>

        <div class="content">
            <!-- Health Endpoint -->
            <div class="endpoint">
                <div class="endpoint-header">
                    <span class="method get">GET</span>
                    <span class="path">/health</span>
                </div>
                <div class="description">Health check endpoint. Returns service status and memory information.</div>
                <button onclick="testHealth()">Try it out</button>
                <div id="health-response"></div>
            </div>

            <!-- Hello Endpoint -->
            <div class="endpoint">
                <div class="endpoint-header">
                    <span class="method get">GET</span>
                    <span class="path">/hello</span>
                </div>
                <div class="description">Simple greeting endpoint with system information.</div>
                <button onclick="testHello()">Try it out</button>
                <div id="hello-response"></div>
            </div>

            <!-- Time Endpoint -->
            <div class="endpoint">
                <div class="endpoint-header">
                    <span class="method get">GET</span>
                    <span class="path">/time</span>
                </div>
                <div class="description">Returns current date and time with system information.</div>
                <button onclick="testTime()">Try it out</button>
                <div id="time-response"></div>
            </div>

            <!-- Math Endpoint -->
            <div class="endpoint">
                <div class="endpoint-header">
                    <span class="method post">POST</span>
                    <span class="path">/math</span>
                </div>
                <div class="description">Performs mathematical operations. Supports: +, -, *, /</div>
                <div class="input-group">
                    <label>Request Body (JSON):</label>
                    <textarea id="math-input" placeholder='{"a": 10, "b": 5, "operation": "+"}'>{
  "a": 10,
  "b": 5,
  "operation": "+"
}</textarea>
                </div>
                <button onclick="testMath()">Try it out</button>
                <div id="math-response"></div>
            </div>
        </div>
    </div>

    <script>
        async function makeRequest(url, method = 'GET', body = null) {
            try {
                const options = {
                    method: method,
                    headers: {}
                };

                if (body) {
                    options.headers['Content-Type'] = 'application/json';
                    options.body = body;
                }

                const response = await fetch(url, options);
                const text = await response.text();

                return {
                    status: response.status,
                    ok: response.ok,
                    body: text
                };
            } catch (error) {
                return {
                    status: 0,
                    ok: false,
                    body: 'Error: ' + error.message
                };
            }
        }

        function displayResponse(elementId, response) {
            const element = document.getElementById(elementId);
            const isSuccess = response.ok;
            const statusClass = isSuccess ? 'success' : 'error';

            let formattedBody;
            try {
                formattedBody = JSON.stringify(JSON.parse(response.body), null, 2);
            } catch {
                formattedBody = response.body;
            }

            element.innerHTML = `
                <div class="response ${isSuccess ? '' : 'error'}">
                    <span class="status ${statusClass}">Status: ${response.status}</span>
                    <pre>${formattedBody}</pre>
                </div>
            `;
        }

        async function testHealth() {
            const response = await makeRequest('/health');
            displayResponse('health-response', response);
        }

        async function testHello() {
            const response = await makeRequest('/hello');
            displayResponse('hello-response', response);
        }

        async function testTime() {
            const response = await makeRequest('/time');
            displayResponse('time-response', response);
        }

        async function testMath() {
            const body = document.getElementById('math-input').value;
            const response = await makeRequest('/math', 'POST', body);
            displayResponse('math-response', response);
        }
    </script>
</body>
</html>
                """;

        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(200, bytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
