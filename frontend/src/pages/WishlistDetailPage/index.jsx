import React, {useEffect, useState} from 'react';
import {Link, useNavigate, useParams} from 'react-router-dom';
import {toast} from 'react-hot-toast';
import {FiArrowLeft, FiHeart, FiPackage} from 'react-icons/fi';
import {useWishlistStore} from '../../store/useWishlistStore';
import {api} from '../../services/api';
import LoadingState from '../../components/LoadingState';
import EmptyImage from '../../components/EmptyImage';
import {formatMoscowDateTime} from '../../utils/formatDate';

const itemKey = (item, idx) => item?.id ?? item?.productId ?? idx;

const WishlistDetailPage = () => {
    const {id} = useParams();
    const navigate = useNavigate();
    const {current, detailLoading, fetchWishlistById, clearCurrent} = useWishlistStore();
    const [products, setProducts] = useState({});

    useEffect(() => {
        fetchWishlistById(id).catch(() => toast.error('Не удалось загрузить список'));
        return () => clearCurrent();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [id]);

    useEffect(() => {
        const items = Array.isArray(current?.items) ? current.items : [];
        const ids = [...new Set(items.map((it) => it.productId).filter(Boolean))];
        if (ids.length === 0) return;

        let cancelled = false;
        Promise.all(
            ids.map((pid) =>
                api.get(`/products/${pid}`)
                    .then(({data}) => [pid, data])
                    .catch(() => [pid, null])
            )
        ).then((pairs) => {
            if (!cancelled) setProducts(Object.fromEntries(pairs));
        });

        return () => {
            cancelled = true;
        };
    }, [current]);

    const items = Array.isArray(current?.items) ? current.items : [];

    return (
        <div className="container mx-auto px-4 py-8 max-w-5xl">
            <button
                onClick={() => navigate('/wishlists')}
                className="flex items-center gap-2 text-gray-500 hover:text-gray-800 mb-6 transition-colors">
                <FiArrowLeft className="w-4 h-4"/>
                К спискам желаний
            </button>

            {detailLoading ? (
                <LoadingState message="Загрузка списка…"/>
            ) : !current ? (
                <div className="text-center py-16 bg-white rounded-2xl border border-gray-100">
                    <FiHeart className="w-12 h-12 text-gray-300 mx-auto mb-4"/>
                    <p className="text-gray-500">Список не найден</p>
                </div>
            ) : (
                <>
                    <div className="flex items-center gap-3 mb-8">
                        <div className="w-12 h-12 bg-gradient-to-r from-green-500 to-emerald-600 rounded-xl flex items-center justify-center">
                            <FiHeart className="w-6 h-6 text-white"/>
                        </div>
                        <div>
                            <h1 className="text-2xl font-bold text-gray-800">{current.name}</h1>
                            <p className="text-sm text-gray-500">
                                {items.length > 0 ? `Товаров: ${items.length}` : 'Список пуст'}
                                {current.createdAt && ` · создан ${formatMoscowDateTime(current.createdAt, 'DD.MM.YYYY')}`}
                            </p>
                        </div>
                    </div>

                    {items.length === 0 ? (
                        <div className="text-center py-16 bg-white rounded-2xl border border-gray-100">
                            <FiPackage className="w-12 h-12 text-gray-300 mx-auto mb-4"/>
                            <p className="text-gray-500 mb-1">В этом списке пока нет товаров</p>
                            <p className="text-sm text-gray-400">
                                Добавление товаров не поддерживается бэкендом: у user-service нет ручки для items
                            </p>
                        </div>
                    ) : (
                        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
                            {items.map((item, idx) => {
                                const product = item.productId ? products[item.productId] : null;
                                const firstImage = product?.imagesUrl?.[0];
                                const title = product?.name || item.name || 'Товар';

                                const card = (
                                    <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden hover:shadow-md transition-shadow h-full flex flex-col">
                                        <div className="h-40 bg-gray-50 flex items-center justify-center overflow-hidden">
                                            {firstImage ? (
                                                <img src={firstImage} alt={title} className="w-full h-full object-cover"/>
                                            ) : (
                                                <EmptyImage size="sm"/>
                                            )}
                                        </div>
                                        <div className="p-4 flex-1 flex flex-col">
                                            <h3 className="font-medium text-gray-800 line-clamp-2" title={title}>{title}</h3>
                                            {product?.brand && (
                                                <p className="text-sm text-gray-500 mt-1">{product.brand}</p>
                                            )}
                                            <div className="mt-auto pt-3">
                                                {product ? (
                                                    <span className="font-semibold text-emerald-600">
                                                        {Number(product.price).toLocaleString('ru-RU')} ₽
                                                    </span>
                                                ) : (
                                                    <span className="text-xs text-gray-400">
                                                        Товар недоступен в каталоге
                                                    </span>
                                                )}
                                            </div>
                                        </div>
                                    </div>
                                );

                                return product ? (
                                    <Link key={itemKey(item, idx)} to={`/products/${item.productId}`}>{card}</Link>
                                ) : (
                                    <div key={itemKey(item, idx)}>{card}</div>
                                );
                            })}
                        </div>
                    )}
                </>
            )}
        </div>
    );
};

export default WishlistDetailPage;
