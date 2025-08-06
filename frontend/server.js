// server.js
const { createServer } = require("https");
const { parse } = require("url");
const next = require("next");
const fs = require("fs");
const path = require("path");

const dev = process.env.NODE_ENV !== "production";
const app = next({ dev });
const handle = app.getRequestHandler();

// HTTPS 옵션: 인증서 경로는 동일하지만, 내용은 이제 IP 주소도 포함합니다.
const httpsOptions = {
  key: fs.readFileSync(path.resolve(__dirname, "certs/localhost-key.pem")),
  cert: fs.readFileSync(path.resolve(__dirname, "certs/localhost.pem")),
};

const port = 3000;
const hostname = "0.0.0.0"; // <-- 1. 모든 네트워크 인터페이스에서 수신하도록 설정

app.prepare().then(() => {
  createServer(httpsOptions, (req, res) => {
    const parsedUrl = parse(req.url, true);
    handle(req, res, parsedUrl);
  }).listen(port, hostname, (err) => {
    // <-- 2. listen 함수에 hostname 추가
    if (err) throw err;
    // 접속 가능한 모든 주소를 안내
    console.log(`> 🚀 Ready on https://localhost:${port}`);
  });
});
