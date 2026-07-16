import {StrictMode} from 'react'
import {createRoot} from 'react-dom/client'
import './index.css'
import App from './App.jsx'
import keycloak, {AUTH_ENABLED, initKeycloak} from './services/auth/keycloak'
import {useAuthStore} from './store/useAuthStore'

const render = () =>
    createRoot(document.getElementById('root')).render(
        <StrictMode>
            <App/>
        </StrictMode>,
    );

async function bootstrap() {
    if (AUTH_ENABLED) {
        try {
            const authenticated = await initKeycloak();
            useAuthStore.getState().syncAuth(authenticated);

            keycloak.onAuthSuccess = () => useAuthStore.getState().syncAuth(true);
            keycloak.onAuthRefreshSuccess = () => useAuthStore.getState().syncAuth(true);
            keycloak.onAuthLogout = () => useAuthStore.getState().syncAuth(false);
            keycloak.onTokenExpired = () => keycloak.updateToken(30);
        } catch (e) {
            console.error('[Keycloak] init failed', e);
        }
    }
    render();
}

bootstrap();
