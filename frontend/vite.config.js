import {defineConfig} from 'vite'
import react from '@vitejs/plugin-react-swc'

const SERVER_IP = '144.124.250.82';

export default defineConfig(({mode}) => {
    const isDev = mode === 'development'

    return {
        plugins: [
            react({
                devTarget: 'es2022',
            })
        ],

        server: isDev
            ? {
                port: 3000,
                host: '0.0.0.0',
                open: false,

                hmr: {
                    overlay: false,
                    clientPort: 3000,
                    host: 'v693663.hosted-by-vdsina.com'
                },

                watch: {
                    usePolling: true,
                    interval: 100,
                },

                cors: true,

                allowedHosts: [
                    'localhost',
                    '127.0.0.1',
                    '0.0.0.0',
                    'v693663.hosted-by-vdsina.com'
                ]
            }
            : undefined,

        build: {
            target: 'es2022',
            sourcemap: true,
            minify: 'esbuild',
        },

        optimizeDeps: {
            include: [
                'react',
                'react-dom',
                'react-router-dom',
                'axios',
                'zustand'
            ],
            exclude: [],
            force: false,
        },

        resolve: {
            alias: {
                '@': '/src',
                '@components': '/src/components',
                '@pages': '/src/pages',
                '@store': '/src/store',
                '@services': '/src/services',
            }
        }
    }
})