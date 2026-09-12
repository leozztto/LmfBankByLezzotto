/** @type {import('next').NextConfig} */

// Cabeçalhos de segurança aplicados a todas as respostas. Defesa em profundidade —
// o nginx à frente pode reforçar, mas a app declara o mínimo por conta própria.
// A CSP começa em Report-Only: a app é mesma-origem (o BFF em /api fala com o
// backend server-side), então 'self' cobre quase tudo; 'unsafe-inline' em style
// é necessário enquanto o Next injeta <style> e usamos CSS-in-JS do Radix.
const securityHeaders = [
  { key: "X-Content-Type-Options", value: "nosniff" },
  { key: "X-Frame-Options", value: "DENY" },
  { key: "Referrer-Policy", value: "strict-origin-when-cross-origin" },
  {
    key: "Permissions-Policy",
    value: "camera=(), microphone=(), geolocation=()",
  },
  {
    key: "Content-Security-Policy-Report-Only",
    value: [
      "default-src 'self'",
      "script-src 'self' 'unsafe-inline' 'unsafe-eval'",
      "style-src 'self' 'unsafe-inline'",
      "img-src 'self' data: blob:",
      "font-src 'self' data:",
      "connect-src 'self'",
      "base-uri 'self'",
      "form-action 'self'",
      "frame-ancestors 'none'",
    ].join("; "),
  },
];

const nextConfig = {
  // Runtime enxuto para o container (node server.js). ADR 0003.
  output: "standalone",
  poweredByHeader: false,
  reactStrictMode: true,
  async headers() {
    return [{ source: "/:path*", headers: securityHeaders }];
  },
};

export default nextConfig;
