import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite';
import basicSsl from '@vitejs/plugin-basic-ssl';
// https://vite.dev/config/
export default defineConfig(({ mode }) => {
    // SSL is enabled only if explicitly requested (e.g. for mobile LAN testing: npm run dev:ssl)
    // By default, standard HTTP (http://localhost:5173) is used to avoid "Your connection is not private" (ERR_CERT_AUTHORITY_INVALID).
    // Note: Modern browsers (Chrome, Edge, Firefox) treat http://localhost as a secure context natively,
    // so camera/mic permissions work perfectly without self-signed certificate errors.
    const enableSsl = mode === 'ssl' || process.env.VITE_SSL === 'true';
    return {
        plugins: [
            tailwindcss(),
            react(),
            ...(enableSsl ? [basicSsl()] : []),
        ],
        define: {
            global: 'window',
        },
        server: {
            port: 5173,
            host: '0.0.0.0',
            proxy: {
                '/api': {
                    target: 'http://localhost:8080',
                    changeOrigin: true,
                    secure: false,
                },
                '/uploads': {
                    target: 'http://localhost:8080',
                    changeOrigin: true,
                    secure: false,
                },
                '/ws': {
                    target: 'http://localhost:8080',
                    ws: true,
                    secure: false,
                }
            }
        }
    };
});
