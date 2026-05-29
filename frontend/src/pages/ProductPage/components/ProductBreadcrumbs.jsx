import React from 'react';
import {Link} from 'react-router-dom';

const ProductBreadcrumbs = ({productName}) => (
    <nav className="flex items-center text-sm text-gray-600 mb-6">
        <Link to="/" className="hover:text-emerald-600">Главная</Link>
        <span className="mx-2">/</span>
        <Link to="/products" className="hover:text-emerald-600">Товары</Link>
        <span className="mx-2">/</span>
        <span className="text-gray-900 font-medium line-clamp-1">{productName}</span>
    </nav>
);

export default ProductBreadcrumbs;
