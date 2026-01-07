import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_URL;

export const realApi = axios.create({
    baseURL: API_BASE_URL,
});

realApi.interceptors.response.use(
    (response) => response,
    (error) => {
        const message = error.response?.data?.message || error.message || 'Ошибка сервера';
        console.error('[API Error]', message);
        return Promise.reject(new Error(message));
    }
);