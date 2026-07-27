import {create} from 'zustand';
import {api} from '../services/api';

export const useWishlistStore = create((set, get) => ({
    wishlists: [],
    loading: false,
    loaded: false,
    error: null,
    current: null,
    detailLoading: false,
    detailError: null,

    fetchWishlists: async () => {
        set({loading: true, error: null});
        try {
            const {data} = await api.get('/wishlists');
            set({wishlists: Array.isArray(data) ? data : [], loading: false, loaded: true});
            return data;
        } catch (e) {
            set({loading: false, error: e?.message || 'Не удалось загрузить списки'});
            throw e;
        }
    },

    ensureWishlists: async () => {
        if (get().loaded || get().loading) return;
        try {
            await get().fetchWishlists();
        } catch {
            // индикация не критична — молча
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

    addProduct: async (wishlistId, productId) => {
        const {data} = await api.post(`/wishlists/${wishlistId}/products/${productId}`);
        set((state) => ({
            current: state.current?.id === data?.id ? data : state.current,
            wishlists: state.wishlists.map((w) => (w.id === data?.id ? data : w)),
        }));
        return data;
    },

    removeProduct: async (wishlistId, productId) => {
        const {data} = await api.delete(`/wishlists/${wishlistId}/products/${productId}`);
        set((state) => ({
            current: state.current?.id === data?.id ? data : state.current,
            wishlists: state.wishlists.map((w) => (w.id === data?.id ? data : w)),
        }));
        return data;
    },
}));
