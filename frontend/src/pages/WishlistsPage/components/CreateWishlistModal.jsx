import React, {useState} from 'react';
import {FiHeart, FiX} from 'react-icons/fi';

const CreateWishlistModal = ({onSubmit, onCancel, initialName = '', title = 'Новый список желаний'}) => {
    const [name, setName] = useState(initialName);
    const [busy, setBusy] = useState(false);
    const [error, setError] = useState('');

    const handleSubmit = async (e) => {
        e.preventDefault();
        const trimmed = name.trim();
        if (!trimmed) {
            setError('Введите название списка');
            return;
        }
        if (trimmed.length > 50) {
            setError('Название не длиннее 50 символов');
            return;
        }
        setBusy(true);
        setError('');
        try {
            await onSubmit(trimmed);
        } finally {
            setBusy(false);
        }
    };

    return (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4" onClick={onCancel}>
            <div className="bg-white rounded-2xl shadow-xl max-w-md w-full p-6" onClick={(e) => e.stopPropagation()}>
                <div className="flex items-center justify-between mb-5">
                    <div className="flex items-center gap-3">
                        <div className="w-11 h-11 bg-gradient-to-r from-green-500 to-emerald-600 rounded-xl flex items-center justify-center">
                            <FiHeart className="w-5 h-5 text-white"/>
                        </div>
                        <h2 className="text-xl font-bold text-gray-800">{title}</h2>
                    </div>
                    <button onClick={onCancel} className="p-2 text-gray-400 hover:text-gray-700 rounded-lg hover:bg-gray-100 transition-colors">
                        <FiX className="w-5 h-5"/>
                    </button>
                </div>

                <form onSubmit={handleSubmit}>
                    <label className="block text-sm font-medium text-gray-700 mb-2">Название</label>
                    <input
                        autoFocus
                        value={name}
                        onChange={(e) => setName(e.target.value)}
                        maxLength={50}
                        placeholder="Например: Подарки на день рождения"
                        className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-emerald-500 focus:border-emerald-500 outline-none transition-all"
                    />
                    {error && <p className="mt-2 text-sm text-red-600">{error}</p>}

                    <div className="flex justify-end gap-3 mt-6">
                        <button type="button" onClick={onCancel} disabled={busy}
                            className="px-5 py-2.5 text-gray-700 font-medium hover:bg-gray-100 rounded-lg transition-colors disabled:opacity-50">
                            Отмена
                        </button>
                        <button type="submit" disabled={busy}
                            className="flex items-center gap-2 px-5 py-2.5 font-medium rounded-lg bg-emerald-600 text-white hover:bg-emerald-700 disabled:opacity-50 transition-colors">
                            {busy ? 'Сохранение…' : 'Сохранить'}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
};

export default CreateWishlistModal;
