import React, {useEffect, useMemo, useState} from 'react';
import {toast} from 'react-hot-toast';
import {FiHeart, FiPlus, FiSearch} from 'react-icons/fi';
import {useWishlistStore} from '../../store/useWishlistStore';
import LoadingState from '../../components/LoadingState';
import ConfirmDialog from '../../components/ui/ConfirmDialog';
import CreateWishlistModal from './components/CreateWishlistModal';
import WishlistCard from './components/WishlistCard';

const WishlistsPage = () => {
    const {wishlists, loading, fetchWishlists, createWishlist, updateWishlist, deleteWishlist} = useWishlistStore();

    const [search, setSearch] = useState('');
    const [showCreate, setShowCreate] = useState(false);
    const [editing, setEditing] = useState(null);
    const [deleting, setDeleting] = useState(null);

    useEffect(() => {
        fetchWishlists().catch(() => toast.error('Не удалось загрузить списки желаний'));
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    const filtered = useMemo(() => {
        const q = search.trim().toLowerCase();
        if (!q) return wishlists;
        return wishlists.filter((w) => String(w.name ?? '').toLowerCase().includes(q));
    }, [wishlists, search]);

    const handleCreate = async (name) => {
        try {
            await createWishlist(name);
            toast.success('Список создан');
            setShowCreate(false);
        } catch (err) {
            toast.error(err?.message || 'Не удалось создать список');
        }
    };

    const handleRename = async (name) => {
        try {
            await updateWishlist(editing.id, name);
            toast.success('Список переименован');
            setEditing(null);
        } catch (err) {
            toast.error(err?.message || 'Не удалось переименовать список');
        }
    };

    const handleDelete = async () => {
        try {
            await deleteWishlist(deleting.id);
            toast.success('Список удалён');
            setDeleting(null);
        } catch (err) {
            toast.error(err?.message || 'Не удалось удалить список');
        }
    };

    return (
        <div className="container mx-auto px-4 py-8 max-w-5xl">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-8">
                <div className="flex items-center gap-3">
                    <div className="w-12 h-12 bg-gradient-to-r from-green-500 to-emerald-600 rounded-xl flex items-center justify-center">
                        <FiHeart className="w-6 h-6 text-white"/>
                    </div>
                    <div>
                        <h1 className="text-2xl font-bold text-gray-800">Списки желаний</h1>
                        <p className="text-sm text-gray-500">Ваши личные подборки товаров</p>
                    </div>
                </div>
                <button
                    onClick={() => setShowCreate(true)}
                    className="flex items-center justify-center gap-2 px-5 py-3 font-medium rounded-lg bg-emerald-600 text-white hover:bg-emerald-700 transition-colors">
                    <FiPlus className="w-5 h-5"/>
                    Новый список
                </button>
            </div>

            <div className="relative mb-8">
                <FiSearch className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400"/>
                <input
                    value={search}
                    onChange={(e) => setSearch(e.target.value)}
                    placeholder="Поиск по названию…"
                    className="w-full pl-11 pr-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-emerald-500 focus:border-emerald-500 outline-none transition-all"
                />
            </div>

            {loading ? (
                <LoadingState message="Загрузка списков…"/>
            ) : wishlists.length === 0 ? (
                <div className="text-center py-16 bg-white rounded-2xl border border-gray-100">
                    <FiHeart className="w-12 h-12 text-gray-300 mx-auto mb-4"/>
                    <p className="text-gray-500 mb-4">Пока нет ни одного списка желаний</p>
                    <button onClick={() => setShowCreate(true)} className="text-emerald-600 font-medium hover:underline">
                        Создать первый список
                    </button>
                </div>
            ) : filtered.length === 0 ? (
                <div className="text-center py-16 bg-white rounded-2xl border border-gray-100">
                    <FiHeart className="w-12 h-12 text-gray-300 mx-auto mb-4"/>
                    <p className="text-gray-500">Ничего не найдено по запросу</p>
                </div>
            ) : (
                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
                    {filtered.map((w, idx) => (
                        <WishlistCard
                            key={w.id ?? `${w.name}-${idx}`}
                            wishlist={w}
                            onEdit={setEditing}
                            onDelete={setDeleting}
                        />
                    ))}
                </div>
            )}

            {showCreate && (
                <CreateWishlistModal onSubmit={handleCreate} onCancel={() => setShowCreate(false)}/>
            )}

            {editing && (
                <CreateWishlistModal
                    title="Переименовать список"
                    initialName={editing.name}
                    onSubmit={handleRename}
                    onCancel={() => setEditing(null)}
                />
            )}

            {deleting && (
                <ConfirmDialog
                    title="Удалить список?"
                    message={`Список «${deleting.name}» будет удалён без возможности восстановления.`}
                    onConfirm={handleDelete}
                    onCancel={() => setDeleting(null)}
                />
            )}
        </div>
    );
};

export default WishlistsPage;
