import React from 'react';
import {FiPackage} from 'react-icons/fi';

const EmptyCategory = () => {
    return (
        <div className="flex items-center text-sm text-emerald-600 bg-emerald-50 px-4 py-3 rounded-lg">
            <FiPackage className="mr-3 flex-shrink-0"/>
            <div>
                <p className="font-medium">Товары скоро появятся</p>
                <p className="text-emerald-500">
                    В этой категории пока нет товаров
                </p>
            </div>
        </div>
    );
};

export default EmptyCategory;