import React from 'react';
import {Link} from 'react-router-dom';
import {FiEdit2, FiTrash2, FiHeart, FiPackage} from 'react-icons/fi';

const WishlistCard = ({wishlist, onEdit, onDelete}) => {
    const products = Array.isArray(wishlist.products) ? wishlist.products : [];
    const hasId = wishlist.id !== undefined && wishlist.id !== null;

    return (
        <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-5 hover:shadow-md transition-shadow flex flex-col">
            <div className="flex items-start justify-between gap-3">
                <div className="flex items-center gap-3 min-w-0">
                    <div className="w-10 h-10 shrink-0 bg-emerald-50 rounded-xl flex items-center justify-center">
                        <FiHeart className="w-5 h-5 text-emerald-600"/>
                    </div>
                    {hasId ? (
                        <Link to={`/wishlists/${wishlist.id}`} className="min-w-0">
                            <h3 className="font-semibold text-gray-800 truncate hover:text-emerald-600 transition-colors">{wishlist.name}</h3>
                        </Link>
                    ) : (
                        <h3 className="font-semibold text-gray-800 truncate">{wishlist.name}</h3>
                    )}
                </div>
                <div className="flex items-center gap-1 shrink-0">
                    <button
                        onClick={() => onEdit(wishlist)}
                        disabled={!hasId}
                        title={hasId ? 'Переименовать' : 'Бэкенд не вернул id списка'}
                        className="p-2 text-gray-400 hover:text-emerald-600 hover:bg-emerald-50 rounded-lg transition-colors disabled:opacity-30 disabled:cursor-not-allowed">
                        <FiEdit2 className="w-4 h-4"/>
                    </button>
                    <button
                        onClick={() => onDelete(wishlist)}
                        disabled={!hasId}
                        title={hasId ? 'Удалить' : 'Бэкенд не вернул id списка'}
                        className="p-2 text-gray-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors disabled:opacity-30 disabled:cursor-not-allowed">
                        <FiTrash2 className="w-4 h-4"/>
                    </button>
                </div>
            </div>

            <div className="mt-4 pt-4 border-t border-gray-100">
                <div className="flex items-center gap-2 text-sm text-gray-500">
                    <FiPackage className="w-4 h-4"/>
                    {products.length > 0
                        ? `Товаров: ${products.length}`
                        : 'Список пуст'}
                </div>
                {products.length > 0 && (
                    <ul className="mt-3 space-y-1.5">
                        {products.slice(0, 4).map((p, idx) => (
                            <li key={p.id ?? idx} className="text-sm text-gray-700 truncate">
                                • {p.name || p.id}
                            </li>
                        ))}
                        {products.length > 4 && (
                            <li className="text-sm text-gray-400">и ещё {products.length - 4}…</li>
                        )}
                    </ul>
                )}
            </div>
        </div>
    );
};

export default WishlistCard;
