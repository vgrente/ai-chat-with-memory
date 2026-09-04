// file path: angular-ai/proxy.conf.js
const PROXY_CONFIG = [
  {
    context: ["/api/chat"],
    target: "http://localhost:8080/",
    secure: false,
    logLevel: "debug",
  },
];
module.exports = PROXY_CONFIG;
