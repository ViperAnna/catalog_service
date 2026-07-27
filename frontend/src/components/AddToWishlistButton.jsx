import React, {useEffect, useMemo, useState} from 'react';
import {toast} from 'react-hot-toast';
import {FiHeart, FiX, FiPlus, FiCheck, FiLoader} from 'react-icons/fi';
import {useWishlistStore} from '../store/useWishlistStore';
import {useAuthStore} from '../store/useAuthStore';

const AddToWishlistModal = ({productId, onClose}) => {
    const {wishlists, loading, fetchWishlists, addProduct, removeProduct, createWishlist} = useWishlistStore();
    const [pending, setPending] = useState(null);
    const [newName, setNewName] = useState('');
    const [creating, setCreating] = useState(false);

    useEffect(() => {
        fetchWishlists().catch(() => toast.error('Не удалось загрузить списки'));
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    const inList = (w) => Array.isArray(w.products) && w.products.some((p) => p.id === productId);

    const handleToggle = async (w) => {
        if (pending) return;
        setPending(w.id);
        try {
            if (inList(w)) {
                await removeProduct(w.id, productId);
                toast.success(`Убрано из «${w.name}»`);
            } else {
                await addProduct(w.id, productId);
                toast.success(`Добавлено в «${w.name}»`);
            }
        } catch (err) {
            toast.error(err?.message || 'Не удалось изменить список');
        } finally {
            setPending(null);
        }
    };

    const handleCreate = async (e) => {
        e.preventDefault();
        const name = newName.trim();
        if (!name || creating) return;
        setCreating(true);
        try {
            await createWishlist(name);
            const created = useWishlistStore.getState().wishlists.find((w) => w.name === name);
            if (created) {
                await addProduct(created.id, productId);
                toast.success(`Создан список «${name}» с товаром`);
            } else {
                toast.success(`Список «${name}» создан`);
            }
            setNewName('');
        } catch (err) {
            toast.error(err?.message || 'Не удалось создать список');
        } finally {
            setCreating(false);
        }
    };

    return (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4" onClick={onClose}>
            <div className="bg-white rounded-2xl shadow-xl max-w-md w-full p-6 max-h-[85vh] flex flex-col" onClick={(e) => e.stopPropagation()}>
                <div className="flex items-center justify-between mb-5">
                    <div className="flex items-center gap-3">
                        <div className="w-11 h-11 bg-gradient-to-r from-green-500 to-emerald-600 rounded-xl flex items-center justify-center">
                            <FiHeart className="w-5 h-5 text-white"/>
                        </div>
                        <h2 className="text-xl font-bold text-gray-800">В список желаний</h2>
                    </div>
                    <button onClick={onClose} className="p-2 text-gray-400 hover:text-gray-700 rounded-lg hover:bg-gray-100 transition-colors">
                        <FiX className="w-5 h-5"/>
                    </button>
                </div>

                <div className="flex-1 overflow-y-auto -mx-1 px-1">
                    {loading && wishlists.length === 0 ? (
                        <div className="flex items-center justify-center py-10 text-gray-400">
                            <FiLoader className="w-5 h-5 animate-spin"/>
                        </div>
                    ) : wishlists.length === 0 ? (
                        <p className="text-sm text-gray-500 text-center py-6">У вас пока нет списков — создайте первый ниже.</p>
                    ) : (
                        <ul className="space-y-2">
                            {wishlists.map((w) => {
                                const active = inList(w);
                                const busy = pending === w.id;
                                return (
                                    <li key={w.id}>
                                        <button
                                            onClick={() => handleToggle(w)}
                                            disabled={busy}
                                            className={`w-full flex items-center justify-between gap-3 px-4 py-3 rounded-lg border transition-colors ${
                                                active
                                                    ? 'border-emerald-500 bg-emerald-50'
                                                    : 'border-gray-200 hover:border-emerald-300 hover:bg-gray-50'
                                            } disabled:opacity-50`}>
                                            <span className="min-w-0 text-left">
                                                <span className="block font-medium text-gray-800 truncate">{w.name}</span>
                                                <span className="block text-xs text-gray-400">
                                                    Товаров: {Array.isArray(w.products) ? w.products.length : 0}
                                                </span>
                                            </span>
                                            {busy ? (
                                                <FiLoader className="w-5 h-5 text-gray-400 animate-spin shrink-0"/>
                                            ) : active ? (
                                                <span className="flex items-center gap-1 text-emerald-600 text-sm font-medium shrink-0">
                                                    <FiCheck className="w-4 h-4"/> В списке
                                                </span>
                                            ) : (
                                                <FiPlus className="w-5 h-5 text-gray-400 shrink-0"/>
                                            )}
                                        </button>
                                    </li>
                                );
                            })}
                        </ul>
                    )}
                </div>

                <form onSubmit={handleCreate} className="mt-5 pt-5 border-t border-gray-100 flex gap-2">
                    <input
                        value={newName}
                        onChange={(e) => setNewName(e.target.value)}
                        maxLength={50}
                        placeholder="Новый список…"
                        className="flex-1 px-4 py-2.5 border border-gray-300 rounded-lg focus:ring-2 focus:ring-emerald-500 focus:border-emerald-500 outline-none transition-all"
                    />
                    <button
                        type="submit"
                        disabled={!newName.trim() || creating}
                        className="flex items-center gap-1.5 px-4 py-2.5 font-medium rounded-lg bg-emerald-600 text-white hover:bg-emerald-700 disabled:opacity-50 transition-colors shrink-0">
                        <FiPlus className="w-4 h-4"/>
                        Создать
                    </button>
                </form>
            </div>
        </div>
    );
};

const AddToWishlistButton = ({productId, variant = 'icon'}) => {
    const authenticated = useAuthStore((s) => s.authenticated);
    const [open, setOpen] = useState(false);

    if (!authenticated) return null;

    const handleOpen = (e) => {
        e.preventDefault();
        e.stopPropagation();
        setOpen(true);
    };

    return (
        <>
            {variant === 'full' ? (
                <button
                    onClick={handleOpen}
                    className="inline-flex items-center gap-2 px-5 py-2.5 font-medium rounded-lg border border-emerald-600 text-emerald-700 hover:bg-emerald-50 transition-colors">
                    <FiHeart className="w-5 h-5"/>
                    В список желаний
                </button>
            ) : (
                <button
                    onClick={handleOpen}
                    title="В список желаний"
                    className="p-2 bg-white/90 text-gray-500 hover:text-emerald-600 rounded-full shadow-sm transition-colors">
                    <FiHeart className="w-5 h-5"/>
                </button>
            )}

            {open && <AddToWishlistModal productId={productId} onClose={() => setOpen(false)}/>}
        </>
    );
};

export default AddToWishlistButton;
