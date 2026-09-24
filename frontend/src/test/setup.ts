import '@testing-library/jest-dom/vitest';
import { cleanup, configure } from '@testing-library/react';
import { afterEach, vi } from 'vitest';

// As páginas renderizam tabelas e modais do Mantine: com todos os arquivos em paralelo, as
// esperas por elementos (findBy*/waitFor) podem passar do padrão de 1 s.
configure({ asyncUtilTimeout: 5000 });

// APIs do navegador usadas pelo Mantine e ausentes no jsdom.
Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: vi.fn((query: string) => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: vi.fn(),
    removeListener: vi.fn(),
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
    dispatchEvent: vi.fn(),
  })),
});

class ResizeObserverFalso {
  observe(): void {
    // sem efeito no jsdom
  }
  unobserve(): void {
    // sem efeito no jsdom
  }
  disconnect(): void {
    // sem efeito no jsdom
  }
}
window.ResizeObserver = ResizeObserverFalso;

// document.fonts (usado pelo Textarea com autosize) não existe no jsdom.
if (!('fonts' in document)) {
  Object.defineProperty(document, 'fonts', {
    value: { addEventListener: vi.fn(), removeEventListener: vi.fn() },
  });
}

afterEach(() => {
  cleanup();
  sessionStorage.clear();
});
