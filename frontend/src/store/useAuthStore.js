import {create} from 'zustand';
import keycloak, {AUTH_ENABLED} from '../services/auth/keycloak';
import {api} from '../services/api';
import {buildUserFormData} from '../utils/userForm';

const extractProfile = () => {
    const t = keycloak?.tokenParsed;
    if (!t) return null;
    return {
        username: t.preferred_username,
        email: t.email,
        firstName: t.given_name,
        lastName: t.family_name,
        fullName: t.name,
        roles: t.realm_access?.roles ?? [],
    };
};

export const useAuthStore = create((set, get) => ({
    authEnabled: AUTH_ENABLED,
    authenticated: false,
    profile: null,
    dbProfile: null,
    loadingProfile: false,

    syncAuth: (authenticated) => {
        set({
            authenticated,
            profile: authenticated ? extractProfile() : null,
            dbProfile: authenticated ? get().dbProfile : null,
        });
    },

    login: () => keycloak?.login(),

    logout: () => keycloak?.logout({redirectUri: window.location.origin}),

    hasRole: (role) => Boolean(keycloak?.hasRealmRole?.(role)),

    fetchMe: async () => {
        if (!AUTH_ENABLED) return null;
        set({loadingProfile: true});
        try {
            const response = await api.get('/users/me');
            set({dbProfile: response.data, loadingProfile: false});
            return response.data;
        } catch (e) {
            set({loadingProfile: false});
            throw e;
        }
    },

    updateMe: async (data) => {
        return await api.put('/users/me', buildUserFormData(data));
    },

    deleteMe: async () => {
        return await api.delete('/users/me');
    },
}));
