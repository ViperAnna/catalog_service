import React from 'react';
import {Link} from 'react-router-dom';
import {FiArrowLeft} from 'react-icons/fi';

const CategoryNotFound = () => {
    return (
        <div className="container mx-auto px-4 py-12">
            <div className="text-center">
                <div className="text-emerald-600 bg-emerald-50 p-6 rounded-lg max-w-md mx-auto">
                    <p className="text-lg font-semibold mb-2">Категория не найдена</p>
                    <p className="text-gray-600 mb-4">
                        Такой категории не существует или она была удалена
                    </p>
                    <Link
                        to="/"
                        className="inline-flex items-center px-4 py-2 bg-emerald-600 text-white rounded-lg hover:bg-emerald-700"
                    >
                        <FiArrowLeft className="mr-2"/>
                        Вернуться на главную
                    </Link>
                </div>
            </div>
        </div>
    );
};

export default CategoryNotFound;