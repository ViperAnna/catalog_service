import React from 'react';
import {FiPackage} from 'react-icons/fi';

const EmptyState = ({hasFilters}) => (
    <div className="flex flex-col items-center justify-center py-20 text-center">
        <div className="w-20 h-20 bg-emerald-50 rounded-full flex items-center justify-center mb-6">
            <FiPackage className="w-10 h-10 text-emerald-400"/>
        </div>
        <h3 className="text-xl font-semibold text-gray-900 mb-2">
            {hasFilters ? 'Товары не найдены' : 'Товаров пока нет'}
        </h3>
        <p className="text-gray-500 max-w-sm">
            {hasFilters
                ? 'Попробуйте изменить фильтры или поисковый запрос'
                : 'Добавьте первый товар, нажав кнопку «Добавить товар»'
            }
        </p>
    </div>
);

export default EmptyState;
