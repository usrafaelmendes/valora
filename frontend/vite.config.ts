import react from '@vitejs/plugin-react';
import { defineConfig } from 'vitest/config';

// Endereço do backend local: destino do proxy /api (web) e URL base da API no modo desktop.
const BACKEND_URL = 'http://localhost:8080';

// Modo "desktop" (npm run dev:desktop / build:desktop, usados pelo Tauri): sem proxy do Vite
// no app empacotado, o frontend chama o backend local diretamente (CORS liberado no backend
// somente para as origens do Tauri e do Vite). Nos demais modos continua usando /api.
export default defineConfig(({ mode }) => ({
  plugins: [react()],
  define:
    mode === 'desktop'
      ? { 'import.meta.env.VITE_API_BASE_URL': JSON.stringify(BACKEND_URL) }
      : undefined,
  server: {
    port: 5173,
    // Versão web: o frontend chama /api/...; o proxy repassa ao backend sem o prefixo,
    // na mesma origem para o navegador.
    proxy: {
      '/api': {
        target: BACKEND_URL,
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, ''),
      },
    },
  },
  test: {
    environment: 'jsdom',
    // Somente os testes de unidade e de componentes; os E2E (e2e/) rodam no Playwright.
    include: ['src/**/*.test.{ts,tsx}'],
    setupFiles: ['./src/test/setup.ts'],
    restoreMocks: true,
    unstubGlobals: true,
    css: false,
    // Os testes de página simulam a digitação e a escolha em listas do Mantine: com todos os
    // arquivos em paralelo, alguns passam de 5 s em máquinas mais lentas.
    testTimeout: 15_000,
  },
}));
