import {create} from 'zustand';
import {api} from '../services/api';

export const useWishlistStore = create((set, get) => ({
    wishlists: [],
    loading: false,
    error: null,
    current: null,
    detailLoading: false,
    detailError: null,

    fetchWishlists: async () => {
        set({loading: true, error: null});
        try {
            const {data} = await api.get('/wishlists');
            set({wishlists: Array.isArray(data) ? data : [], loading: false});
            return data;
        } catch (e) {
            set({loading: false, error: e?.message || 'Не удалось загрузить списки'});
            throw e;
        }
    },

    searchWishlists: async (name) => {
        const q = (name ?? '').trim();
        if (!q) return get().fetchWishlists();
        set({loading: true, error: null});
        try {
            const {data} = await api.get(`/wishlists/search?name=${encodeURIComponent(q)}`);
            set({wishlists: Array.isArray(data) ? data : [], loading: false});
            return data;
        } catch (e) {
            set({loading: false, error: e?.message || 'Ошибка поиска'});
            throw e;
        }
    },

    fetchWishlistById: async (id) => {
        set({detailLoading: true, detailError: null, current: null});
        try {
            const {data} = await api.get(`/wishlists/${id}`);
            set({current: data ?? null, detailLoading: false});
            return data;
        } catch (e) {
            set({detailLoading: false, detailError: e?.message || 'Не удалось загрузить список'});
            throw e;
        }
    },

    clearCurrent: () => set({current: null, detailError: null}),

    createWishlist: async (name) => {
        const response = await api.post('/wishlists', {name: name.trim()});
        await get().fetchWishlists();
        return response;
    },

    updateWishlist: async (id, name) => {
        const response = await api.put(`/wishlists/${id}`, {name: name.trim()});
        await get().fetchWishlists();
        return response;
    },

    deleteWishlist: async (id) => {
        const response = await api.delete(`/wishlists/${id}`);
        set((state) => ({wishlists: state.wishlists.filter((w) => w.id !== id)}));
        return response;
    },
}));
