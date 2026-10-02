import React from 'react';
import ReactDOM from 'react-dom/client';
import { Provider } from 'react-redux';
import { BrowserRouter } from 'react-router-dom';
import App from './App';
import { store } from './app/store';
import './index.css';

// Applies the saved theme as early as the app module runs, so the very first
// React render already uses the correct palette. Reads the same localStorage key
// the navbar toggle writes, and falls back to light — the OS colour scheme is
// deliberately not consulted. Note: this is a deferred module, so the initial
// paint is governed by the default :root tokens; removing the remaining flash
// would need a blocking inline script in index.html.
try {
  const stored = window.localStorage.getItem('shopsphere-theme');
  document.documentElement.setAttribute('data-theme', stored === 'dark' ? 'dark' : 'light');
} catch {
  document.documentElement.setAttribute('data-theme', 'light');
}

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <Provider store={store}>
      <BrowserRouter>
        <App />
      </BrowserRouter>
    </Provider>
  </React.StrictMode>,
);