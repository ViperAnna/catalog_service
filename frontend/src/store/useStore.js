import {create} from 'zustand';
import {api} from '../services/api';

const MINIO_INTERNAL_URL = 'http://minio:9000';
// const MINIO_EXTERNAL_URL = 'http://localhost:9000';
const MINIO_EXTERNAL_URL = `${window.location.protocol}//${window.location.hostname}/minio`;

const convertPictureUrl = (url) => {
    if (!url) return null;
    return url.replace(MINIO_INTERNAL_URL, MINIO_EXTERNAL_URL);
};

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
            const categoriesWithConvertedUrls = response.data.map(category => ({
                ...category,
                imageUrl: convertPictureUrl(category.imageUrl)
            }));
            set({categories: categoriesWithConvertedUrls, loading: false});
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
            const category = response.data
            const convertedCategory = {
                ...category,
                imageUrl: convertPictureUrl(category.imageUrl)
            };
            set({currentCategory: convertedCategory, loading: false});
        } catch (e) {
            set({error: e.message, loading: false});
        }
    },

    createCategory: async (categoryData, options = {}) => {
        return await api.post('/categories', categoryData, {
            // headers: {
            //     'Content-Type': 'multipart/form-data',
            // },
            ...options
            // ,
        });
    },

    updateCategory: async (id, categoryData, options = {}) => {
        return await api.put(`/categories/${id}`, categoryData, {
            // headers: {
            //     'Content-Type': 'multipart/form-data',
            // },
            ...options
            // ,
        });
    },

    deleteCategory: async (id) => {
        return await api.delete(`/categories/${id}`);
    },

    // ========== ТОВАРЫ ==========

    pagination: {page: 0, size: 8, totalPages: 0, totalElements: 0},

    fetchProducts: async (params = {}) => {
        const {page = 0, size = 8, categoryId} = params;
        set({loading: true, error: null});
        try {
            const query = new URLSearchParams({page, size});
            if (categoryId) query.append('categoryId', categoryId);
            const response = await api.get(`/products?${query}`);
            const data = response.data;
            const productsWithUrls = data.content.map(product => ({
                ...product,
                imagesUrl: product.imagesUrl?.map(url => convertPictureUrl(url)) || []
            }));
            set({
                products: productsWithUrls,
                pagination: {
                    page: data.number,
                    size: data.size,
                    totalPages: data.totalPages,
                    totalElements: data.totalElements
                },
                loading: false
            });
        } catch (e) {
            set({error: e.message, loading: false});
        }
    },

    // Количество товаров в категории. Бэкенд не отдаёт счётчик в категории,
    // поэтому берём totalElements из постраничного ответа /products?categoryId=
    fetchCategoryProductCount: async (categoryId) => {
        if (!categoryId) return 0;
        try {
            const response = await api.get(`/products?categoryId=${categoryId}&size=1`);
            return response.data?.totalElements ?? 0;
        } catch {
            return 0;
        }
    },

    fetchProductsByName: async (name) => {
        set({loading: true, error: null});
        try {
            const response = await api.get(`/products/productByName/${encodeURIComponent(name)}`);
            const productsWithUrls = response.data.map(product => ({
                ...product,
                imagesUrl: product.imagesUrl?.map(url => convertPictureUrl(url)) || []
            }));
            set({products: productsWithUrls, loading: false});
        } catch (e) {
            set({error: e.message, loading: false});
        }
    },

    fetchProductById: async (id) => {
        set({loading: true, error: null});
        try {
            const response = await api.get(`/products/${id}`);
            const product = response.data;
            const convertedProduct = {
                ...product,
                imagesUrl: product.imagesUrl?.map(url => convertPictureUrl(url)) || []
            };
            set({currentProduct: convertedProduct, loading: false});
        } catch (e) {
            set({error: e.message, loading: false});
        }
    },

    createProduct: async (productData, options = {}) => {
        return await api.post('/products', productData, options);
    },

    updateProduct: async (id, productData, options = {}) => {
        return await api.put(`/products/${id}`, productData, options);
    },

    deleteProduct: async (id) => {
        return await api.delete(`/products/${id}`);
    },

    clearCurrentProduct: () => set({currentProduct: null}),
}));