import React from 'react';
import {formatMoscowDateTime} from '../../../../utils/formatDate.js';

const CategoryStats = ({category, productCount}) => {
    const stats = [
        {
            label: 'Товаров',
            value: productCount === null || productCount === undefined ? '…' : String(productCount),
            isNumber: true
        },
        {
            label: 'Создана',
            value: formatMoscowDateTime(category.createdAt),
            isDate: true
        },
        {
            label: 'Обновлена',
            value: formatMoscowDateTime(category.updatedAt),
            isDate: true
        }
    ];

    return (
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-8">
            {stats.map((stat, index) => (
                <div key={index} className="bg-gray-50 p-4 rounded-lg text-center">
                    {stat.isNumber ? (
                        <div className="text-2xl font-bold text-gray-900">{stat.value}</div>
                    ) : (
                        <>
                            <div className="text-sm text-gray-600">{stat.label}</div>
                            <div className="font-medium">{stat.value}</div>
                        </>
                    )}
                    {!stat.isDate && <div className="text-sm text-gray-600">{stat.label}</div>}
                </div>
            ))}
        </div>
    );
};

export default CategoryStats;