import React from 'react';
import {Link} from 'react-router-dom';
import {FiArrowRight, FiPackage} from 'react-icons/fi';

const EmptyCategory = ({categoryId}) => {
    return (
        <div className="flex items-center justify-between text-sm text-emerald-600 bg-emerald-50 px-4 py-3 rounded-lg">
            <div className="flex items-center">
                <FiPackage className="mr-3 flex-shrink-0"/>
                <div>
                    <p className="font-medium">Товары скоро появятся</p>
                    <p className="text-emerald-500">В этой категории пока нет товаров</p>
                </div>
            </div>
            {categoryId && (
                <Link
                    to={`/products?categoryId=${categoryId}`}
                    className="flex items-center gap-1 ml-4 text-emerald-700 hover:text-emerald-900 font-medium whitespace-nowrap"
                >
                    Перейти к товарам
                    <FiArrowRight className="w-4 h-4"/>
                </Link>
            )}
        </div>
    );
};

export default EmptyCategory;
