import {create} from 'zustand';
import {api} from '../services/api';
import {buildUserFormData} from '../utils/userForm';

export const userKeycloakId = (user) => user?.keycloakUserId ?? user?.keycloakId ?? null;

export const useAdminStore = create((set, get) => ({
    users: [],
    loading: false,
    error: null,

    fetchUsers: async () => {
        set({loading: true, error: null});
        try {
            const {data} = await api.get('/admin/users');
            set({users: Array.isArray(data) ? data : [], loading: false});
            return data;
        } catch (e) {
            set({loading: false, error: e?.message || 'Не удалось загрузить пользователей'});
            throw e;
        }
    },

    updateUser: async (keycloakId, data) => {
        const response = await api.put(`/admin/users/${keycloakId}`, buildUserFormData(data));
        await get().fetchUsers();
        return response;
    },

    deleteUser: async (keycloakId) => {
        const response = await api.delete(`/admin/users/${keycloakId}`);
        set((state) => ({users: state.users.filter((u) => userKeycloakId(u) !== keycloakId)}));
        return response;
    },
}));
