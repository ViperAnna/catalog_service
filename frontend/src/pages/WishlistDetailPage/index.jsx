import React, {useEffect, useState} from 'react';
import {Link, useNavigate, useParams} from 'react-router-dom';
import {toast} from 'react-hot-toast';
import {FiArrowLeft, FiHeart, FiPackage, FiTrash2} from 'react-icons/fi';
import {useWishlistStore} from '../../store/useWishlistStore';
import LoadingState from '../../components/LoadingState';
import EmptyImage from '../../components/EmptyImage';
import ConfirmDialog from '../../components/ui/ConfirmDialog';
import {formatMoscowDateTime} from '../../utils/formatDate';

const WishlistDetailPage = () => {
    const {id} = useParams();
    const navigate = useNavigate();
    const {current, detailLoading, fetchWishlistById, clearCurrent, removeProduct} = useWishlistStore();
    const [removing, setRemoving] = useState(null);
    const [busy, setBusy] = useState(false);

    useEffect(() => {
        fetchWishlistById(id).catch(() => toast.error('Не удалось загрузить список'));
        return () => clearCurrent();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [id]);

    const products = Array.isArray(current?.products) ? current.products : [];

    const handleRemove = async () => {
        if (busy || !removing) return;
        setBusy(true);
        try {
            await removeProduct(current.id, removing.id);
            toast.success('Товар удалён из списка');
            setRemoving(null);
        } catch (err) {
            toast.error(err?.message || 'Не удалось удалить товар');
        } finally {
            setBusy(false);
        }
    };

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
                                {products.length > 0 ? `Товаров: ${products.length}` : 'Список пуст'}
                                {current.createdAt && ` · создан ${formatMoscowDateTime(current.createdAt, 'DD.MM.YYYY')}`}
                            </p>
                        </div>
                    </div>

                    {products.length === 0 ? (
                        <div className="text-center py-16 bg-white rounded-2xl border border-gray-100">
                            <FiPackage className="w-12 h-12 text-gray-300 mx-auto mb-4"/>
                            <p className="text-gray-500 mb-1">В этом списке пока нет товаров</p>
                            <Link to="/products" className="text-sm text-emerald-600 font-medium hover:underline">
                                Перейти в каталог
                            </Link>
                        </div>
                    ) : (
                        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
                            {products.map((product) => {
                                const firstImage = product.imagesUrl?.[0];
                                const title = product.name || 'Товар';

                                return (
                                    <div key={product.id} className="relative bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden hover:shadow-md transition-shadow flex flex-col">
                                        <button
                                            onClick={() => setRemoving(product)}
                                            title="Удалить из списка"
                                            className="absolute top-2 right-2 z-10 p-2 bg-white/90 text-gray-400 hover:text-red-600 rounded-lg shadow-sm transition-colors">
                                            <FiTrash2 className="w-4 h-4"/>
                                        </button>
                                        <Link to={`/products/${product.id}`} className="flex-1 flex flex-col">
                                            <div className="h-40 bg-gray-50 flex items-center justify-center overflow-hidden">
                                                {firstImage ? (
                                                    <img src={firstImage} alt={title} className="w-full h-full object-cover"/>
                                                ) : (
                                                    <EmptyImage size="sm"/>
                                                )}
                                            </div>
                                            <div className="p-4 flex-1 flex flex-col">
                                                <h3 className="font-medium text-gray-800 line-clamp-2" title={title}>{title}</h3>
                                                {product.articleNumber && (
                                                    <p className="text-xs text-gray-400 mt-1">Арт. {product.articleNumber}</p>
                                                )}
                                                <div className="mt-auto pt-3">
                                                    <span className="font-semibold text-emerald-600">
                                                        {Number(product.price).toLocaleString('ru-RU')} ₽
                                                    </span>
                                                </div>
                                            </div>
                                        </Link>
                                    </div>
                                );
                            })}
                        </div>
                    )}
                </>
            )}

            {removing && (
                <ConfirmDialog
                    title="Удалить товар из списка?"
                    message={`Товар «${removing.name || removing.id}» будет удалён из списка «${current?.name}».`}
                    onConfirm={handleRemove}
                    onCancel={() => setRemoving(null)}
                />
            )}
        </div>
    );
};

export default WishlistDetailPage;
