/** @type {import('next').NextConfig} */
const nextConfig = {
  // Runtime enxuto para o container (node server.js). ADR 0003.
  output: "standalone",

  // Só em desenvolvimento (`next dev`): faz o proxy de /api para o backend, para
  // que o front possa rodar com HMR na :3000 sem subir o nginx e sem CORS —
  // mantém o contrato "mesma origem" da ADR 0003. Em produção o nginx intercepta
  // /api antes de chegar no Next, então este rewrite nunca dispara.
  async rewrites() {
    const backend = process.env.BACKEND_ORIGIN ?? "http://localhost:8080";
    return [{ source: "/api/:path*", destination: `${backend}/:path*` }];
  },
};

export default nextConfig;
