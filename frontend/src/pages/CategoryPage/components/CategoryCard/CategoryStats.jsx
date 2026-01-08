import React from 'react';

const CategoryStats = ({category}) => {
    const stats = [
        {
            label: 'Товаров',
            value: '0',
            isNumber: true
        },
        {
            label: 'Создана',
            value: new Date(category.createdAt).toLocaleDateString('ru-RU'),
            isDate: true
        },
        {
            label: 'Обновлена',
            value: new Date(category.updatedAt).toLocaleDateString('ru-RU'),
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