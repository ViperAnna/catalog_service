import React from 'react';
import {Link} from 'react-router-dom';
import {FiArrowRight, FiTag} from 'react-icons/fi';
import EmptyImage from '../../../components/EmptyImage.jsx';
import AddToWishlistButton from '../../../components/AddToWishlistButton.jsx';
import {getProductStatusColor, getProductStatusLabel} from '../../../utils/productStatus.js';

const ProductCard = ({product}) => {
    const firstImage = product.imagesUrl?.[0];

    return (
        <Link to={`/products/${product.id}`}>
            <div
                className="group bg-white rounded-xl shadow-md hover:shadow-lg transition-all duration-300 overflow-hidden border border-gray-100 hover:border-green-200">
                <div className="relative h-48 overflow-hidden">
                    {firstImage ? (
                        <img
                            src={firstImage}
                            alt={product.name}
                            className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
                        />
                    ) : (
                        <EmptyImage/>
                    )}
                    <div className="absolute inset-0 bg-gradient-to-t from-black/20 to-transparent"/>
                    <div className="absolute top-3 left-3">
                        <AddToWishlistButton productId={product.id}/>
                    </div>
                    <div className="absolute top-3 right-3">
                        <span className={`px-3 py-1 rounded-full text-xs font-medium ${getProductStatusColor(product.status)}`}>
                            {getProductStatusLabel(product.status)}
                        </span>
                    </div>
                    {product.imagesUrl?.length > 1 && (
                        <div className="absolute bottom-2 right-2 bg-black/50 text-white text-xs px-2 py-1 rounded-full">
                            +{product.imagesUrl.length - 1}
                        </div>
                    )}
                </div>

                <div className="p-5">
                    <div className="flex justify-between items-start mb-2">
                        <h3
                            title={product.name}
                            className="text-lg font-bold text-gray-900 group-hover:text-emerald-600 transition-colors line-clamp-2 leading-tight h-[3.2rem]"
                        >
                            {product.name}
                        </h3>
                        <FiArrowRight
                            className="w-5 h-5 text-gray-400 group-hover:text-emerald-600 group-hover:translate-x-1 transition-all flex-shrink-0 ml-2 mt-1"/>
                    </div>

                    <p className="text-sm text-gray-500 mb-3 font-medium">{product.brand}</p>

                    <p
                        title={product.description}
                        className="text-gray-600 mb-4 line-clamp-2 text-sm h-[2.4rem] leading-snug"
                    >
                        {product.description}
                    </p>

                    <div className="flex items-center justify-between pt-4 border-t border-gray-100">
                        <span className="text-xl font-bold text-emerald-600">
                            {Number(product.price).toLocaleString('ru-RU')} ₽
                        </span>
                        {product.tag?.length > 0 && (
                            <div className="flex items-center gap-1 text-xs text-gray-400">
                                <FiTag className="w-3 h-3"/>
                                <span>{product.tag[0]}</span>
                                {product.tag.length > 1 && <span>+{product.tag.length - 1}</span>}
                            </div>
                        )}
                    </div>
                </div>

                <div
                    className="h-1 bg-gradient-to-r from-emerald-500 to-emerald-700 transform scale-x-0 group-hover:scale-x-100 transition-transform duration-300"/>
            </div>
        </Link>
    );
};

export default ProductCard;
