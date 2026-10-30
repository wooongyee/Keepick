// Local browser fixture only; not mounted in the application or deployment.
const http = require("node:http");
const fs = require("node:fs");
const path = require("node:path");
const html = fs.readFileSync(path.join(__dirname, "../tests/signaling-client.html"));
http.createServer((_req, res) => {
  res.writeHead(200, {"Content-Type":"text/html; charset=utf-8","Cache-Control":"no-store"});
  res.end(html);
}).listen(3108, "127.0.0.1", () => console.log("Signaling fixture: http://127.0.0.1:3108"));
