import React from 'react';
import {formatMoscowDateTime} from '../../../../utils/formatDate.js';

const ProductStats = ({product}) => (
    <div className="mt-6 pt-6 border-t border-gray-100 grid grid-cols-2 gap-4 text-sm text-gray-500">
        {product.articleNumber && (
            <div className="col-span-2">
                <span className="font-medium text-gray-700">Артикул: </span>
                <span className="font-mono text-gray-600">{product.articleNumber}</span>
            </div>
        )}
        <div>
            <p className="text-xs text-gray-400 mb-0.5">Создан</p>
            <p className="font-medium text-gray-600">
                {formatMoscowDateTime(product.dateCreate, 'DD.MM.YYYY HH:mm')}
            </p>
        </div>
        <div>
            <p className="text-xs text-gray-400 mb-0.5">Обновлён</p>
            <p className="font-medium text-gray-600">
                {formatMoscowDateTime(product.dateUpdate, 'DD.MM.YYYY HH:mm')}
            </p>
        </div>
    </div>
);

export default ProductStats;
