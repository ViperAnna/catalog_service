import {loadFromLocalStorage, mockData, saveToLocalStorage} from './mockData';

export const mockApi = {
    get: async (url, config = {}) => {
        const delay = 2000;
        await new Promise(resolve => setTimeout(resolve, delay));
        if (url === '/categories') {
            return {data: loadFromLocalStorage()};
        }
        if (url.startsWith('/categories/')) {
            const id = parseInt(url.split('/')[2]);
            const category = loadFromLocalStorage().find(c => c.id === id);
            if (!category) {
                throw new Error(`Category with id ${id} not found`);
            }
            return {data: category};
        }
        if (url === '/products') {
            return {data: mockData.products || []};
        }
        throw new Error(`[MOCK] Endpoint ${url} not implemented`);
    },
    post: async (url, data, config = {}) => {
        await new Promise(resolve => setTimeout(resolve, 2000));
        if (url === '/categories') {
            const name = data.get('name') || '';
            const description = data.get('description') || '';
            const status = data.get('status') || 'active';
            const imageFile = data.get('image');
            const categoryData = {name, description, status};
            let imageUrl;
            if (imageFile && imageFile instanceof File) {
                imageUrl = await new Promise((resolve, reject) => {
                    const reader = new FileReader();
                    reader.onload = (e) => resolve(e.target.result);
                    reader.onerror = reject;
                    reader.readAsDataURL(imageFile);
                });
            }

            const newCategory = {
                id: Date.now(),
                ...categoryData,
                image: imageUrl,
                createdAt: new Date().toISOString(),
                updatedAt: new Date().toISOString()
            };
            mockData.categories.push(newCategory);
            saveToLocalStorage();
            return {
                data: newCategory,
                status: 201,
            };
        }
        throw new Error(`[MOCK] Endpoint ${url} not implemented`);
    },
    put: async (url, data, config = {}) => {
        console.log('zashli')
        await new Promise(resolve => setTimeout(resolve, 2000));

        if (url.startsWith('/categories/')) {
            const id = parseInt(url.split('/')[2]);
            const index = mockData.categories.findIndex(c => c.id === id);

            if (index === -1) {
                throw new Error(`Category with id ${id} not found`);
            }

            const name = data.get('name') || '';
            const description = data.get('description') || '';
            const status = data.get('status') || 'active';
            const imageFile = data.get('image');

            let imageUrl = mockData.categories[index].image;

            if (imageFile === '') {
                imageUrl = null;
            } else if (imageFile && imageFile instanceof File) {
                imageUrl = await new Promise((resolve, reject) => {
                    const reader = new FileReader();
                    reader.onload = (e) => resolve(e.target.result);
                    reader.onerror = reject;
                    reader.readAsDataURL(imageFile);
                });
            }

            const updatedCategory = {
                ...mockData.categories[index],
                name,
                description,
                status,
                image: imageUrl,
                updatedAt: new Date().toISOString()
            };
            mockData.categories[index] = updatedCategory;
            saveToLocalStorage();
            return {
                data: updatedCategory,
                status: 200,
            };
        }

        throw new Error(`[MOCK] Endpoint ${url} not implemented`);
    },
    delete: async (url, config = {}) => {
        await new Promise(resolve => setTimeout(resolve, 2000));

        if (url.startsWith('/categories/')) {
            const id = parseInt(url.split('/')[2]);
            const index = mockData.categories.findIndex(c => c.id === id);

            if (index === -1) {
                return {
                    data: null,
                    status: 404,
                    message: `Category with id ${id} not found`
                };
            }

            mockData.categories.splice(index, 1);
            saveToLocalStorage();

            return {
                status: 204,
            };
        }

        throw new Error(`[MOCK] Endpoint ${url} not implemented`);
    },

    interceptors: {
        response: {
            use: () => {
            }
        }
    }
};