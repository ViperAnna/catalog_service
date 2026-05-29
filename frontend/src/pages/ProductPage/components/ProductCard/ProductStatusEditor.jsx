import React from 'react';
import {PRODUCT_STATUS, getProductStatusLabel} from '../../../../utils/productStatus.js';
import {getProductStatusColor} from '../../../../utils/productStatus.js';

const ProductStatusEditor = ({isEditing, editedStatus, isSubmitting, onStatusChange}) => {
    if (!isEditing) {
        return (
            <div className="mb-4">
                <span className={`px-3 py-1.5 rounded-full text-sm font-medium ${getProductStatusColor(editedStatus)}`}>
                    {getProductStatusLabel(editedStatus)}
                </span>
            </div>
        );
    }

    return (
        <div className="mb-6">
            <label className="block text-sm font-medium text-gray-700 mb-2">Статус товара</label>
            <div className="grid grid-cols-2 gap-2">
                {Object.values(PRODUCT_STATUS).map(status => (
                    <button
                        key={status}
                        type="button"
                        onClick={() => onStatusChange(status)}
                        disabled={isSubmitting}
                        className={`px-3 py-2 rounded-lg border-2 text-sm font-medium transition-all disabled:opacity-50 disabled:cursor-not-allowed ${
                            editedStatus === status
                                ? 'border-emerald-500 bg-emerald-50 text-emerald-700'
                                : 'border-gray-300 bg-white text-gray-700 hover:bg-gray-50'
                        }`}
                    >
                        {getProductStatusLabel(status)}
                    </button>
                ))}
            </div>
        </div>
    );
};

export default ProductStatusEditor;
