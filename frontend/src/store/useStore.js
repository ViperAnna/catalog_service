import {create} from 'zustand';
import {api} from '../services/api';

export const useStore = create((set, get) => ({
    categories: [],
    currentCategory: null,

    products: [],
    currentProduct: null,
    searchQuery: '',

    loading: false,
    error: null,

    fetchCategories: async () => {
        const state = get();
        if (state.loading) {
            return;
        }
        set({loading: true, error: null});
        try {
            const response = await api.get('/categories');
            set({categories: response.data, loading: false});
        } catch (e) {
            set({error: e.message, loading: false})
        }
    },

    fetchCategoryById: async (id) => {
        const state = get();
        if (state.loading) {
            return;
        }
        set({loading: true, error: null});
        try {
            const response = await api.get(`/categories/${id}`);
            set({currentCategory: response.data, loading: false});
        } catch (e) {
            set({error: e.message, loading: false});
        }
    },

    createCategory: async (categoryData) => {
        return await api.post('/categories', categoryData)
    },

    updateCategory: async (id, categoryData) => {
        return await api.put(`/categories/${id}`, categoryData);
    },

    deleteCategory: async (id) => {
        return await api.delete(`/categories/${id}`);
    },

    // // ========== ТОВАРЫ ==========
    //
    // // Получить все товары или поиск
    // fetchProducts: async (search = '') => {
    //     set({loading: true, error: null, searchQuery: search});
    //     try {
    //         const url = search
    //             ? `/products/search?q=${encodeURIComponent(search)}`
    //             : '/products';
    //         const response = await api.get(url);
    //         set({products: response.data, loading: false});
    //     } catch (error) {
    //         set({error: error.message, loading: false});
    //     }
    // },
    //
    // // Получить товар по ID
    // fetchProductById: async (id) => {
    //     set({loading: true, error: null});
    //     try {
    //         const response = await api.get(`/products/${id}`);
    //         set({currentProduct: response.data, loading: false});
    //         return response.data;
    //     } catch (error) {
    //         set({error: error.message, loading: false});
    //         throw error;
    //     }
    // },
    //
    // // Получить товары категории
    // fetchProductsByCategory: async (categoryId) => {
    //     set({loading: true, error: null});
    //     try {
    //         const response = await api.get(`/categories/${categoryId}/products`);
    //         set({products: response.data, loading: false});
    //     } catch (error) {
    //         set({error: error.message, loading: false});
    //     }
    // },
    //
    // // Создать товар
    // createProduct: async (productData) => {
    //     set({loading: true, error: null});
    //     try {
    //         const response = await api.post('/products', productData);
    //         const newProduct = response.data;
    //         set(state => ({
    //             products: [...state.products, newProduct],
    //             loading: false
    //         }));
    //         return newProduct;
    //     } catch (error) {
    //         set({error: error.message, loading: false});
    //         throw error;
    //     }
    // },
    //
    // // Обновить товар
    // updateProduct: async (id, productData) => {
    //     set({loading: true, error: null});
    //     try {
    //         const response = await api.put(`/products/${id}`, productData);
    //         const updatedProduct = response.data;
    //         set(state => ({
    //             products: state.products.map(prod =>
    //                 prod.id === id ? updatedProduct : prod
    //             ),
    //             currentProduct: state.currentProduct?.id === id
    //                 ? updatedProduct
    //                 : state.currentProduct,
    //             loading: false
    //         }));
    //         return updatedProduct;
    //     } catch (error) {
    //         set({error: error.message, loading: false});
    //         throw error;
    //     }
    // },
    //
    // // Удалить товар
    // deleteProduct: async (id) => {
    //     set({loading: true, error: null});
    //     try {
    //         await api.delete(`/products/${id}`);
    //         set(state => ({
    //             products: state.products.filter(prod => prod.id !== id),
    //             currentProduct: state.currentProduct?.id === id
    //                 ? null
    //                 : state.currentProduct,
    //             loading: false
    //         }));
    //     } catch (error) {
    //         set({error: error.message, loading: false});
    //         throw error;
    //     }
    // },
    //
    // // Очистить текущий товар
    // clearCurrentProduct: () => set({currentProduct: null}),
    //
    // // Очистить текущую категорию
    // clearCurrentCategory: () => set({currentCategory: null}),
    //
    // // Очистить ошибку
    // clearError: () => set({error: null})
}));