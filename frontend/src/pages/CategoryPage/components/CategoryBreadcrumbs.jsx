import React from 'react';
import { Link } from 'react-router-dom';

const CategoryBreadcrumbs = ({ categoryName }) => {
    return (
        <nav className="flex items-center text-sm text-gray-600 mb-6">
            <Link to="/" className="hover:text-emerald-600">
                Главная
            </Link>
            <span className="mx-2">/</span>
            <span className="text-gray-900 font-medium">{categoryName}</span>
        </nav>
    );
};

export default CategoryBreadcrumbs;