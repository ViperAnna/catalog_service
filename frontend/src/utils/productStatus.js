export const PRODUCT_STATUS = {
    ACTIVE: 'ACTIVE',
    INACTIVE: 'INACTIVE',
    ARCHIVED: 'ARCHIVED',
    DRAFT: 'DRAFT',
};

export const getProductStatusLabel = (status) => {
    switch (status) {
        case 'ACTIVE': return 'Активный';
        case 'INACTIVE': return 'Неактивный';
        case 'ARCHIVED': return 'В архиве';
        case 'DRAFT': return 'Черновик';
        default: return status;
    }
};

export const getProductStatusColor = (status) => {
    switch (status) {
        case 'ACTIVE': return 'bg-green-100 text-green-800';
        case 'INACTIVE': return 'bg-red-100 text-red-800';
        case 'ARCHIVED': return 'bg-amber-100 text-amber-800';
        case 'DRAFT': return 'bg-gray-100 text-gray-700';
        default: return 'bg-gray-100 text-gray-700';
    }
};
