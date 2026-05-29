export const PRODUCT_STATUS = {
    IN_STOCK: 'IN_STOCK',
    OUT_OF_STOCK: 'OUT_OF_STOCK',
    PRE_ORDER: 'PRE_ORDER',
    DRAFT: 'DRAFT',
};

export const getProductStatusLabel = (status) => {
    switch (status) {
        case 'IN_STOCK': return 'В наличии';
        case 'OUT_OF_STOCK': return 'Нет в наличии';
        case 'PRE_ORDER': return 'Предзаказ';
        case 'DRAFT': return 'Черновик';
        default: return status;
    }
};

export const getProductStatusColor = (status) => {
    switch (status) {
        case 'IN_STOCK': return 'bg-green-100 text-green-800';
        case 'OUT_OF_STOCK': return 'bg-red-100 text-red-800';
        case 'PRE_ORDER': return 'bg-blue-100 text-blue-800';
        case 'DRAFT': return 'bg-gray-100 text-gray-700';
        default: return 'bg-gray-100 text-gray-700';
    }
};
