import React from 'react';
import {Link} from 'react-router-dom';
import {FiFolder} from 'react-icons/fi';
import {useStore} from '../../../../store/useStore.js';

const ProductCategories = ({isEditing, selectedIds, isSubmitting, onToggle, displayCategories}) => {
    const {categories} = useStore();

    if (!isEditing) {
        if (!displayCategories?.length) return null;
        return (
            <div className="mb-4">
                <h4 className="text-sm font-semibold text-gray-700 mb-2 flex items-center gap-1">
                    <FiFolder className="w-4 h-4"/>Категории
                </h4>
                <div className="flex flex-wrap gap-2">
                    {displayCategories.map(cat => (
                        <Link
                            key={cat.id}
                            to={`/products?categoryId=${cat.id}`}
                            className="px-3 py-1 bg-blue-50 text-blue-700 rounded-full text-sm font-medium hover:bg-blue-100 transition-colors"
                        >
                            {cat.name}
                        </Link>
                    ))}
                </div>
            </div>
        );
    }

    return (
        <div className="mb-6">
            <h4 className="text-sm font-semibold text-gray-700 mb-2">Категории</h4>
            <div className="grid grid-cols-2 gap-2 max-h-40 overflow-y-auto border border-gray-200 rounded-lg p-3">
                {categories.map(cat => (
                    <label key={cat.id} className="flex items-center gap-2 cursor-pointer group">
                        <input
                            type="checkbox"
                            checked={selectedIds.includes(cat.id)}
                            onChange={() => onToggle(cat.id)}
                            disabled={isSubmitting}
                            className="w-4 h-4 text-emerald-600 focus:ring-emerald-500 rounded"
                        />
                        <span className="text-sm text-gray-700 group-hover:text-emerald-700 transition-colors line-clamp-1">
                            {cat.name}
                        </span>
                    </label>
                ))}
            </div>
        </div>
    );
};

export default ProductCategories;
