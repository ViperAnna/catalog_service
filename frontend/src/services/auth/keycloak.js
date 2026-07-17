import Keycloak from 'keycloak-js';

const url = import.meta.env.VITE_KEYCLOAK_URL;
const realm = import.meta.env.VITE_KEYCLOAK_REALM;
const clientId = import.meta.env.VITE_KEYCLOAK_CLIENT_ID;
const isMock = import.meta.env.VITE_USE_MOCK === 'true';

export const AUTH_ENABLED = Boolean(url && realm && clientId) && !isMock;

const keycloak = AUTH_ENABLED
    ? new Keycloak({url, realm, clientId})
    : null;

let initPromise = null;

export function initKeycloak() {
    if (!AUTH_ENABLED) return Promise.resolve(false);
    if (initPromise) return initPromise;

    initPromise = keycloak.init({
        onLoad: 'check-sso',
        pkceMethod: 'S256',
        silentCheckSsoRedirectUri: `${window.location.origin}/silent-check-sso.html`,
        checkLoginIframe: false,
    });

    return initPromise;
}

export async function getValidToken(minValiditySeconds = 30) {
    if (!AUTH_ENABLED || !keycloak?.authenticated) return null;
    try {
        await keycloak.updateToken(minValiditySeconds);
        return keycloak.token;
    } catch {
        return null;
    }
}

export default keycloak;
