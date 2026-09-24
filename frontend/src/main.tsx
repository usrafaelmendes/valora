import '@mantine/core/styles.css';
import './styles/global.css';

import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { App } from './app/App';

const raiz = document.getElementById('root');
if (!raiz) {
  throw new Error('Elemento #root não encontrado em index.html.');
}

createRoot(raiz).render(
  <StrictMode>
    <App />
  </StrictMode>,
);
