import React from 'react';
import {Link} from 'react-router-dom';
import {FiArrowLeft, FiPackage} from 'react-icons/fi';

const ProductNotFound = () => (
    <div className="min-h-[65vh] flex items-center justify-center">
        <div className="text-center">
            <div className="w-24 h-24 bg-gray-100 rounded-full flex items-center justify-center mx-auto mb-6">
                <FiPackage className="w-12 h-12 text-gray-400"/>
            </div>
            <h2 className="text-2xl font-bold text-gray-900 mb-2">Товар не найден</h2>
            <p className="text-gray-500 mb-8">Этот товар не существует или был удалён</p>
            <Link
                to="/products"
                className="inline-flex items-center gap-2 px-6 py-3 bg-emerald-600 text-white rounded-lg hover:bg-emerald-700 transition-colors font-medium"
            >
                <FiArrowLeft className="w-4 h-4"/>
                Вернуться к товарам
            </Link>
        </div>
    </div>
);

export default ProductNotFound;
