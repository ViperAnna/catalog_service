import {defineConfig} from 'vite'
import react from '@vitejs/plugin-react-swc'

export default defineConfig({
    plugins: [
        react({
            devTarget: 'es2022',
        })
    ],

    server: {
        port: 3000,
        host: '0.0.0.0',
        open: false,

        hmr: {
            overlay: false,
            clientPort: 3000,
            host: 'localhost'
        },

        watch: {
            usePolling: true,
            interval: 100,
        },

        cors: true,
        historyApiFallback: true,

        allowedHosts: ['localhost', '127.0.0.1', '0.0.0.0']
    },

    build: {
        target: 'es2022',
        sourcemap: true,
        minify: 'terser',
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
})